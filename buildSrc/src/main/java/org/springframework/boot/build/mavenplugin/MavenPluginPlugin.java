/*
 * Copyright 2012-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.build.mavenplugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Properties;
import java.util.function.BiConsumer;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import io.spring.javaformat.formatter.FileEdit;
import io.spring.javaformat.formatter.FileFormatter;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.ComponentMetadataContext;
import org.gradle.api.artifacts.ComponentMetadataRule;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ModuleVersionIdentifier;
import org.gradle.api.artifacts.VariantMetadata;
import org.gradle.api.artifacts.component.ModuleComponentIdentifier;
import org.gradle.api.artifacts.result.ResolvedArtifactResult;
import org.gradle.api.attributes.DocsType;
import org.gradle.api.attributes.Usage;
import org.gradle.api.file.CopySpec;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.plugins.JavaLibraryPlugin;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.publish.PublishingExtension;
import org.gradle.api.publish.maven.MavenPublication;
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.JavaExec;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.Sync;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.TaskExecutionException;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.bundling.Jar;
import org.gradle.api.tasks.javadoc.Javadoc;
import org.gradle.external.javadoc.StandardJavadocDocletOptions;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import org.springframework.boot.build.DeployedPlugin;
import org.springframework.boot.build.MavenRepositoryPlugin;
import org.springframework.boot.build.test.IntegrationTestPlugin;
import org.springframework.core.CollectionFactory;
import org.springframework.util.Assert;

/**
 * Plugin for building Spring Boot's Maven Plugin.
 *
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
public class MavenPluginPlugin implements Plugin<Project> {

	/**
     * Applies the necessary plugins and configures the project for Maven plugin development.
     * 
     * @param project The project to apply the plugins and configure.
     */
    @Override
	public void apply(Project project) {
		project.getPlugins().apply(JavaLibraryPlugin.class);
		project.getPlugins().apply(MavenPublishPlugin.class);
		project.getPlugins().apply(DeployedPlugin.class);
		project.getPlugins().apply(MavenRepositoryPlugin.class);
		project.getPlugins().apply(IntegrationTestPlugin.class);
		Jar jarTask = (Jar) project.getTasks().getByName(JavaPlugin.JAR_TASK_NAME);
		configurePomPackaging(project);
		addPopulateIntTestMavenRepositoryTask(project);
		MavenExec generateHelpMojoTask = addGenerateHelpMojoTask(project, jarTask);
		MavenExec generatePluginDescriptorTask = addGeneratePluginDescriptorTask(project, jarTask,
				generateHelpMojoTask);
		addDocumentPluginGoalsTask(project, generatePluginDescriptorTask);
		addPrepareMavenBinariesTask(project);
		addExtractVersionPropertiesTask(project);
	}

	/**
     * Configures the packaging of the POM file for the given project.
     * 
     * @param project the project to configure
     */
    private void configurePomPackaging(Project project) {
		PublishingExtension publishing = project.getExtensions().getByType(PublishingExtension.class);
		publishing.getPublications().withType(MavenPublication.class, this::setPackaging);
	}

	/**
     * Sets the packaging of the given MavenPublication to "maven-plugin".
     * 
     * @param mavenPublication the MavenPublication to set the packaging for
     */
    private void setPackaging(MavenPublication mavenPublication) {
		mavenPublication.pom((pom) -> pom.setPackaging("maven-plugin"));
	}

	/**
     * Adds and populates the IntTestMavenRepository task to the MavenPluginPlugin class.
     * 
     * @param project The project to which the task is added and populated.
     */
    private void addPopulateIntTestMavenRepositoryTask(Project project) {
		Configuration runtimeClasspathWithMetadata = project.getConfigurations().create("runtimeClasspathWithMetadata");
		runtimeClasspathWithMetadata
			.extendsFrom(project.getConfigurations().getByName(JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME));
		runtimeClasspathWithMetadata.attributes((attributes) -> attributes.attribute(DocsType.DOCS_TYPE_ATTRIBUTE,
				project.getObjects().named(DocsType.class, "maven-repository")));
		RuntimeClasspathMavenRepository runtimeClasspathMavenRepository = project.getTasks()
			.create("runtimeClasspathMavenRepository", RuntimeClasspathMavenRepository.class);
		runtimeClasspathMavenRepository.getOutputDirectory()
			.set(new File(project.getBuildDir(), "runtime-classpath-repository"));
		project.getDependencies()
			.components((components) -> components.all(MavenRepositoryComponentMetadataRule.class));
		Sync task = project.getTasks().create("populateIntTestMavenRepository", Sync.class);
		task.setDestinationDir(new File(project.getBuildDir(), "int-test-maven-repository"));
		task.with(copyIntTestMavenRepositoryFiles(project, runtimeClasspathMavenRepository));
		task.dependsOn(project.getTasks().getByName(MavenRepositoryPlugin.PUBLISH_TO_PROJECT_REPOSITORY_TASK_NAME));
		project.getTasks().getByName(IntegrationTestPlugin.INT_TEST_TASK_NAME).dependsOn(task);
	}

	/**
     * Copies the files from the Maven repository to the specified destination.
     * 
     * @param project The project to which the Maven repository belongs.
     * @param runtimeClasspathMavenRepository The runtime classpath Maven repository.
     * @return The copy specification for the Maven repository files.
     */
    private CopySpec copyIntTestMavenRepositoryFiles(Project project,
			RuntimeClasspathMavenRepository runtimeClasspathMavenRepository) {
		CopySpec copySpec = project.copySpec();
		copySpec.from(project.getConfigurations().getByName(MavenRepositoryPlugin.MAVEN_REPOSITORY_CONFIGURATION_NAME));
		copySpec.from(new File(project.getBuildDir(), "maven-repository"));
		copySpec.from(runtimeClasspathMavenRepository);
		return copySpec;
	}

	/**
     * Adds a task to generate documentation for plugin goals.
     * 
     * @param project The project to which the task will be added.
     * @param generatePluginDescriptorTask The task that generates the plugin descriptor.
     */
    private void addDocumentPluginGoalsTask(Project project, MavenExec generatePluginDescriptorTask) {
		DocumentPluginGoals task = project.getTasks().create("documentPluginGoals", DocumentPluginGoals.class);
		File pluginXml = new File(generatePluginDescriptorTask.getOutputs().getFiles().getSingleFile(), "plugin.xml");
		task.setPluginXml(pluginXml);
		task.setOutputDir(new File(project.getBuildDir(), "docs/generated/goals/"));
		task.dependsOn(generatePluginDescriptorTask);
	}

	/**
     * Adds a task to generate the help mojo for the Maven plugin.
     * 
     * @param project the project to add the task to
     * @param jarTask the JAR task to include the help mojo in
     * @return the MavenExec task for generating the help mojo
     */
    private MavenExec addGenerateHelpMojoTask(Project project, Jar jarTask) {
		File helpMojoDir = new File(project.getBuildDir(), "help-mojo");
		MavenExec task = createGenerateHelpMojoTask(project, helpMojoDir);
		task.dependsOn(createSyncHelpMojoInputsTask(project, helpMojoDir));
		includeHelpMojoInJar(jarTask, task);
		return task;
	}

	/**
     * Creates a task for generating the help mojo.
     *
     * @param project The project to create the task for.
     * @param helpMojoDir The directory where the help mojo will be generated.
     * @return The created MavenExec task for generating the help mojo.
     */
    private MavenExec createGenerateHelpMojoTask(Project project, File helpMojoDir) {
		MavenExec task = project.getTasks().create("generateHelpMojo", MavenExec.class);
		task.setProjectDir(helpMojoDir);
		task.args("org.apache.maven.plugins:maven-plugin-plugin:3.6.1:helpmojo");
		task.getOutputs().dir(new File(helpMojoDir, "target/generated-sources/plugin"));
		return task;
	}

	/**
     * Creates a Sync task for syncing the helpMojoDir directory with the project's resources.
     * 
     * @param project The project object representing the Maven project.
     * @param helpMojoDir The directory where the helpMojo inputs will be synced.
     * @return The created Sync task.
     */
    private Sync createSyncHelpMojoInputsTask(Project project, File helpMojoDir) {
		Sync task = project.getTasks().create("syncHelpMojoInputs", Sync.class);
		task.setDestinationDir(helpMojoDir);
		File pomFile = new File(project.getProjectDir(), "src/maven/resources/pom.xml");
		task.from(pomFile, (copy) -> replaceVersionPlaceholder(copy, project));
		return task;
	}

	/**
     * Includes the help mojo in the JAR file.
     * 
     * @param jarTask The JAR task to include the help mojo in.
     * @param generateHelpMojoTask The task to generate the help mojo.
     */
    private void includeHelpMojoInJar(Jar jarTask, JavaExec generateHelpMojoTask) {
		jarTask.from(generateHelpMojoTask).exclude("**/*.java");
		jarTask.dependsOn(generateHelpMojoTask);
	}

	/**
     * Adds a task to generate the plugin descriptor.
     * 
     * @param project The project object.
     * @param jarTask The Jar task object.
     * @param generateHelpMojoTask The MavenExec task object for generating the help mojo.
     * @return The MavenExec task object for generating the plugin descriptor.
     */
    private MavenExec addGeneratePluginDescriptorTask(Project project, Jar jarTask, MavenExec generateHelpMojoTask) {
		File pluginDescriptorDir = new File(project.getBuildDir(), "plugin-descriptor");
		File generatedHelpMojoDir = new File(project.getBuildDir(), "generated/sources/helpMojo");
		SourceSet mainSourceSet = getMainSourceSet(project);
		project.getTasks().withType(Javadoc.class, this::setJavadocOptions);
		FormatHelpMojoSource formattedHelpMojoSource = createFormatHelpMojoSource(project, generateHelpMojoTask,
				generatedHelpMojoDir);
		project.getTasks().getByName(mainSourceSet.getCompileJavaTaskName()).dependsOn(formattedHelpMojoSource);
		mainSourceSet.java((javaSources) -> javaSources.srcDir(formattedHelpMojoSource));
		Sync pluginDescriptorInputs = createSyncPluginDescriptorInputs(project, pluginDescriptorDir, mainSourceSet);
		pluginDescriptorInputs.dependsOn(mainSourceSet.getClassesTaskName());
		MavenExec task = createGeneratePluginDescriptorTask(project, pluginDescriptorDir);
		task.dependsOn(pluginDescriptorInputs);
		includeDescriptorInJar(jarTask, task);
		return task;
	}

	/**
     * Returns the main source set of the given project.
     * 
     * @param project the project for which to retrieve the main source set
     * @return the main source set of the project
     */
    private SourceSet getMainSourceSet(Project project) {
		SourceSetContainer sourceSets = project.getExtensions().getByType(JavaPluginExtension.class).getSourceSets();
		return sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME);
	}

	/**
     * Sets the Javadoc options for the given Javadoc instance.
     *
     * @param javadoc the Javadoc instance to set the options for
     */
    private void setJavadocOptions(Javadoc javadoc) {
		StandardJavadocDocletOptions options = (StandardJavadocDocletOptions) javadoc.getOptions();
		options.addMultilineStringsOption("tag").setValue(Arrays.asList("goal:X", "requiresProject:X", "threadSafe:X"));
	}

	/**
     * Creates a FormatHelpMojoSource object with the given parameters.
     * 
     * @param project The project object.
     * @param generateHelpMojoTask The MavenExec object for generating the help mojo.
     * @param generatedHelpMojoDir The directory where the generated help mojo will be stored.
     * @return The created FormatHelpMojoSource object.
     */
    private FormatHelpMojoSource createFormatHelpMojoSource(Project project, MavenExec generateHelpMojoTask,
			File generatedHelpMojoDir) {
		FormatHelpMojoSource formatHelpMojoSource = project.getTasks()
			.create("formatHelpMojoSource", FormatHelpMojoSource.class);
		formatHelpMojoSource.setGenerator(generateHelpMojoTask);
		formatHelpMojoSource.setOutputDir(generatedHelpMojoDir);
		return formatHelpMojoSource;
	}

	/**
     * Creates a Sync task for syncing plugin descriptor inputs.
     * 
     * @param project the project object
     * @param destination the destination directory for syncing
     * @param sourceSet the source set to sync
     * @return the created Sync task for syncing plugin descriptor inputs
     */
    private Sync createSyncPluginDescriptorInputs(Project project, File destination, SourceSet sourceSet) {
		Sync pluginDescriptorInputs = project.getTasks().create("syncPluginDescriptorInputs", Sync.class);
		pluginDescriptorInputs.setDestinationDir(destination);
		File pomFile = new File(project.getProjectDir(), "src/maven/resources/pom.xml");
		pluginDescriptorInputs.from(pomFile, (copy) -> replaceVersionPlaceholder(copy, project));
		pluginDescriptorInputs.from(sourceSet.getOutput().getClassesDirs(), (sync) -> sync.into("target/classes"));
		pluginDescriptorInputs.from(sourceSet.getAllJava().getSrcDirs(), (sync) -> sync.into("src/main/java"));
		pluginDescriptorInputs.getInputs().property("version", project.getVersion());
		return pluginDescriptorInputs;
	}

	/**
     * Creates a task for generating the plugin descriptor using the Maven Plugin Plugin.
     * 
     * @param project   The project to create the task for.
     * @param mavenDir  The directory where Maven is installed.
     * @return          The created MavenExec task for generating the plugin descriptor.
     */
    private MavenExec createGeneratePluginDescriptorTask(Project project, File mavenDir) {
		MavenExec generatePluginDescriptor = project.getTasks().create("generatePluginDescriptor", MavenExec.class);
		generatePluginDescriptor.args("org.apache.maven.plugins:maven-plugin-plugin:3.6.1:descriptor");
		generatePluginDescriptor.getOutputs().dir(new File(mavenDir, "target/classes/META-INF/maven"));
		generatePluginDescriptor.getInputs()
			.dir(new File(mavenDir, "target/classes/org"))
			.withPathSensitivity(PathSensitivity.RELATIVE)
			.withPropertyName("plugin classes");
		generatePluginDescriptor.setProjectDir(mavenDir);
		return generatePluginDescriptor;
	}

	/**
     * Includes the plugin descriptor in the JAR file.
     * 
     * @param jar The JAR file to include the descriptor in.
     * @param generatePluginDescriptorTask The task that generates the plugin descriptor.
     */
    private void includeDescriptorInJar(Jar jar, JavaExec generatePluginDescriptorTask) {
		jar.from(generatePluginDescriptorTask, (copy) -> copy.into("META-INF/maven/"));
		jar.dependsOn(generatePluginDescriptorTask);
	}

	/**
     * Adds a task to prepare Maven binaries.
     * 
     * @param project the project to add the task to
     */
    private void addPrepareMavenBinariesTask(Project project) {
		TaskProvider<PrepareMavenBinaries> task = project.getTasks()
			.register("prepareMavenBinaries", PrepareMavenBinaries.class, (prepareMavenBinaries) -> prepareMavenBinaries
				.setOutputDir(new File(project.getBuildDir(), "maven-binaries")));
		project.getTasks()
			.getByName(IntegrationTestPlugin.INT_TEST_TASK_NAME)
			.getInputs()
			.dir(task.map(PrepareMavenBinaries::getOutputDir))
			.withPathSensitivity(PathSensitivity.RELATIVE)
			.withPropertyName("mavenBinaries");
	}

	/**
     * Replaces the version placeholder in the given CopySpec with the actual version of the project.
     * 
     * @param copy The CopySpec to apply the version replacement to.
     * @param project The Project object representing the current Maven project.
     */
    private void replaceVersionPlaceholder(CopySpec copy, Project project) {
		copy.filter((input) -> replaceVersionPlaceholder(project, input));
	}

	/**
     * Replaces the placeholder "{{version}}" in the given input string with the version of the project.
     * 
     * @param project the Maven project
     * @param input the input string with the placeholder
     * @return the input string with the placeholder replaced by the project version
     */
    private String replaceVersionPlaceholder(Project project, String input) {
		return input.replace("{{version}}", project.getVersion().toString());
	}

	/**
     * Adds a task to extract version properties.
     * 
     * @param project the project to add the task to
     */
    private void addExtractVersionPropertiesTask(Project project) {
		ExtractVersionProperties extractVersionProperties = project.getTasks()
			.create("extractVersionProperties", ExtractVersionProperties.class);
		extractVersionProperties.setEffectiveBoms(project.getConfigurations().create("versionProperties"));
		extractVersionProperties.getDestination()
			.set(project.getLayout()
				.getBuildDirectory()
				.dir("generated-resources")
				.map((dir) -> dir.file("extracted-versions.properties")));
	}

	/**
     * FormatHelpMojoSource class.
     */
    public static class FormatHelpMojoSource extends DefaultTask {

		private Task generator;

		private File outputDir;

		/**
         * Sets the generator task for the FormatHelpMojoSource class.
         * 
         * @param generator the generator task to be set
         */
        void setGenerator(Task generator) {
			this.generator = generator;
			getInputs().files(this.generator)
				.withPathSensitivity(PathSensitivity.RELATIVE)
				.withPropertyName("generated source");
		}

		/**
         * Returns the output directory for the FormatHelpMojoSource class.
         *
         * @return the output directory
         */
        @OutputDirectory
		public File getOutputDir() {
			return this.outputDir;
		}

		/**
         * Sets the output directory for the generated files.
         * 
         * @param outputDir the output directory to set
         */
        void setOutputDir(File outputDir) {
			this.outputDir = outputDir;
		}

		/**
         * Synchronizes and formats the files.
         * 
         * This method uses a FileFormatter object to format the files generated by the generator.
         * It iterates over each output file and formats it using the UTF-8 character encoding.
         * The formatted files are then saved using the save method.
         */
        @TaskAction
		void syncAndFormat() {
			FileFormatter formatter = new FileFormatter();
			for (File output : this.generator.getOutputs().getFiles()) {
				formatter.formatFiles(getProject().fileTree(output), StandardCharsets.UTF_8)
					.forEach((edit) -> save(output, edit));
			}
		}

		/**
         * Saves the edited file to the specified output location.
         * 
         * @param output The output file to save the edited content.
         * @param edit The file edit containing the edited content.
         * @throws TaskExecutionException If an error occurs while saving the file.
         */
        private void save(File output, FileEdit edit) {
			Path relativePath = output.toPath().relativize(edit.getFile().toPath());
			Path outputLocation = this.outputDir.toPath().resolve(relativePath);
			try {
				Files.createDirectories(outputLocation.getParent());
				Files.writeString(outputLocation, edit.getFormattedContent());
			}
			catch (Exception ex) {
				throw new TaskExecutionException(this, ex);
			}
		}

	}

	/**
     * MavenRepositoryComponentMetadataRule class.
     */
    public static class MavenRepositoryComponentMetadataRule implements ComponentMetadataRule {

		private final ObjectFactory objects;

		/**
         * Constructs a new MavenRepositoryComponentMetadataRule with the specified ObjectFactory.
         *
         * @param objects the ObjectFactory used for dependency injection
         */
        @javax.inject.Inject
		public MavenRepositoryComponentMetadataRule(ObjectFactory objects) {
			this.objects = objects;
		}

		/**
         * Executes the MavenRepositoryComponentMetadataRule.
         * 
         * @param context the ComponentMetadataContext
         */
        @Override
		public void execute(ComponentMetadataContext context) {
			context.getDetails()
				.maybeAddVariant("compileWithMetadata", "compile", (variant) -> configureVariant(context, variant));
			context.getDetails()
				.maybeAddVariant("apiElementsWithMetadata", "apiElements",
						(variant) -> configureVariant(context, variant));
		}

		/**
         * Configures a variant with the given component metadata context and variant metadata.
         * 
         * @param context the component metadata context
         * @param variant the variant metadata
         */
        private void configureVariant(ComponentMetadataContext context, VariantMetadata variant) {
			variant.attributes((attributes) -> {
				attributes.attribute(DocsType.DOCS_TYPE_ATTRIBUTE,
						this.objects.named(DocsType.class, "maven-repository"));
				attributes.attribute(Usage.USAGE_ATTRIBUTE, this.objects.named(Usage.class, "maven-repository"));
			});
			variant.withFiles((files) -> {
				ModuleVersionIdentifier id = context.getDetails().getId();
				files.addFile(id.getName() + "-" + id.getVersion() + ".pom");
			});
		}

	}

	/**
     * RuntimeClasspathMavenRepository class.
     */
    public static class RuntimeClasspathMavenRepository extends DefaultTask {

		private final Configuration runtimeClasspath;

		private final DirectoryProperty outputDirectory;

		/**
         * Constructor for the RuntimeClasspathMavenRepository class.
         * Initializes the runtimeClasspath and outputDirectory properties.
         * 
         * @param runtimeClasspath The runtime classpath configuration.
         * @param outputDirectory The output directory property.
         */
        public RuntimeClasspathMavenRepository() {
			this.runtimeClasspath = getProject().getConfigurations().getByName("runtimeClasspathWithMetadata");
			this.outputDirectory = getProject().getObjects().directoryProperty();
		}

		/**
         * Returns the output directory property of the RuntimeClasspathMavenRepository.
         *
         * @return The output directory property.
         */
        @OutputDirectory
		public DirectoryProperty getOutputDirectory() {
			return this.outputDirectory;
		}

		/**
         * Returns the runtime classpath configuration of the RuntimeClasspathMavenRepository.
         *
         * @return the runtime classpath configuration
         */
        @Classpath
		public Configuration getRuntimeClasspath() {
			return this.runtimeClasspath;
		}

		/**
         * Creates a repository for the artifacts in the runtime classpath.
         * 
         * This method iterates over the resolved artifact results in the runtime classpath and creates a repository location for each artifact.
         * The repository location is determined based on the artifact's group, module, version, and file name.
         * The artifact file is then copied to the repository location.
         * 
         * @throws RuntimeException if there is an error copying the artifact file
         */
        @TaskAction
		public void createRepository() {
			for (ResolvedArtifactResult result : this.runtimeClasspath.getIncoming().getArtifacts()) {
				if (result.getId().getComponentIdentifier() instanceof ModuleComponentIdentifier identifier) {
					String fileName = result.getFile()
						.getName()
						.replace(identifier.getVersion() + "-" + identifier.getVersion(), identifier.getVersion());
					File repositoryLocation = this.outputDirectory
						.dir(identifier.getGroup().replace('.', '/') + "/" + identifier.getModule() + "/"
								+ identifier.getVersion() + "/" + fileName)
						.get()
						.getAsFile();
					repositoryLocation.getParentFile().mkdirs();
					try {
						Files.copy(result.getFile().toPath(), repositoryLocation.toPath(),
								StandardCopyOption.REPLACE_EXISTING);
					}
					catch (IOException ex) {
						throw new RuntimeException("Failed to copy artifact '" + result + "'", ex);
					}
				}
			}
		}

	}

	/**
     * ExtractVersionProperties class.
     */
    public static class ExtractVersionProperties extends DefaultTask {

		private final RegularFileProperty destination;

		private FileCollection effectiveBoms;

		/**
         * Initializes the destination property of the ExtractVersionProperties class.
         */
        public ExtractVersionProperties() {
			this.destination = getProject().getObjects().fileProperty();
		}

		/**
         * Returns the effective BOMs.
         * 
         * @return The effective BOMs as a FileCollection.
         */
        @InputFiles
		@PathSensitive(PathSensitivity.RELATIVE)
		public FileCollection getEffectiveBoms() {
			return this.effectiveBoms;
		}

		/**
         * Sets the effective BOMs for the ExtractVersionProperties class.
         * 
         * @param effectiveBoms the FileCollection representing the effective BOMs
         */
        public void setEffectiveBoms(FileCollection effectiveBoms) {
			this.effectiveBoms = effectiveBoms;
		}

		/**
         * Returns the destination regular file property.
         *
         * @return the destination regular file property
         */
        @OutputFile
		public RegularFileProperty getDestination() {
			return this.destination;
		}

		/**
         * Extracts version properties from the effective BOM and writes them to a file.
         * 
         * @param None
         * @return None
         */
        @TaskAction
		public void extractVersionProperties() {
			EffectiveBom effectiveBom = new EffectiveBom(this.effectiveBoms.getSingleFile());
			Properties versions = extractVersionProperties(effectiveBom);
			writeProperties(versions);
		}

		/**
         * Writes the given properties to a file.
         * 
         * @param versions the properties to be written
         * @throws GradleException if failed to write the properties to the file
         */
        private void writeProperties(Properties versions) {
			File outputFile = this.destination.getAsFile().get();
			outputFile.getParentFile().mkdirs();
			try (Writer writer = new FileWriter(outputFile)) {
				versions.store(writer, null);
			}
			catch (IOException ex) {
				throw new GradleException("Failed to write extracted version properties", ex);
			}
		}

		/**
         * Extracts the version properties from the given EffectiveBom object.
         * 
         * @param effectiveBom the EffectiveBom object containing the version information
         * @return the extracted version properties as a Properties object
         */
        private Properties extractVersionProperties(EffectiveBom effectiveBom) {
			Properties versions = CollectionFactory.createSortedProperties(true);
			versions.setProperty("project.version", effectiveBom.version());
			effectiveBom.property("log4j2.version", versions::setProperty);
			effectiveBom.property("maven-jar-plugin.version", versions::setProperty);
			effectiveBom.property("maven-war-plugin.version", versions::setProperty);
			effectiveBom.property("build-helper-maven-plugin.version", versions::setProperty);
			effectiveBom.property("spring-framework.version", versions::setProperty);
			effectiveBom.property("jakarta-servlet.version", versions::setProperty);
			effectiveBom.property("kotlin.version", versions::setProperty);
			effectiveBom.property("assertj.version", versions::setProperty);
			effectiveBom.property("junit-jupiter.version", versions::setProperty);
			return versions;
		}

	}

	/**
     * EffectiveBom class.
     */
    private static final class EffectiveBom {

		private final Document document;

		private final XPath xpath;

		/**
         * Constructs a new EffectiveBom object by loading the specified BOM file.
         * 
         * @param bomFile the BOM file to be loaded
         */
        private EffectiveBom(File bomFile) {
			this.document = loadDocument(bomFile);
			this.xpath = XPathFactory.newInstance().newXPath();
		}

		/**
         * Loads a document from the given BOM file.
         * 
         * @param bomFile the BOM file to load the document from
         * @return the loaded document
         * @throws IllegalStateException if there is an error parsing the document
         */
        private Document loadDocument(File bomFile) {
			try {
				try (InputStream inputStream = new FileInputStream(bomFile)) {
					DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
					DocumentBuilder builder = builderFactory.newDocumentBuilder();
					return builder.parse(inputStream);
				}
			}
			catch (ParserConfigurationException | SAXException | IOException ex) {
				throw new IllegalStateException(ex);
			}
		}

		/**
         * Returns the version of the EffectiveBom.
         *
         * @return the version of the EffectiveBom
         */
        private String version() {
			return get("version");
		}

		/**
         * Sets the value of a property and applies a handler function to it.
         * 
         * @param name the name of the property
         * @param handler the handler function to be applied to the property value
         */
        private void property(String name, BiConsumer<String, String> handler) {
			handler.accept(name, get("properties/" + name));
		}

		/**
         * Retrieves the value of a specific element in the project XML document using an XPath expression.
         * 
         * @param expression the XPath expression to evaluate
         * @return the text content of the matching node, or null if no matching node is found
         * @throws IllegalStateException if an error occurs while evaluating the XPath expression
         */
        private String get(String expression) {
			try {
				Node node = (Node) this.xpath.compile("/project/" + expression)
					.evaluate(this.document, XPathConstants.NODE);
				String text = (node != null) ? node.getTextContent() : null;
				Assert.hasLength(text, () -> "No result for expression " + expression);
				return text;
			}
			catch (XPathExpressionException ex) {
				throw new IllegalStateException(ex);
			}
		}

	}

}
