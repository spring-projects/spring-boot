/*
 * Copyright 2012-2022 the original author or authors.
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

import io.spring.javaformat.config.IndentationStyle;
import io.spring.javaformat.config.JavaBaseline;
import io.spring.javaformat.config.JavaFormatConfig;
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
import org.gradle.api.plugins.JavaPluginConvention;
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

	private static final JavaFormatConfig FORMATTER_CONFIG = new JavaFormatConfig() {

		@Override
		public JavaBaseline getJavaBaseline() {
			return JavaBaseline.V8;
		}

		@Override
		public IndentationStyle getIndentationStyle() {
			return IndentationStyle.TABS;
		}

	};

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

	private void configurePomPackaging(Project project) {
		PublishingExtension publishing = project.getExtensions().getByType(PublishingExtension.class);
		publishing.getPublications().withType(MavenPublication.class, this::setPackaging);
	}

	private void setPackaging(MavenPublication mavenPublication) {
		mavenPublication.pom((pom) -> pom.setPackaging("maven-plugin"));
	}

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

	private CopySpec copyIntTestMavenRepositoryFiles(Project project,
			RuntimeClasspathMavenRepository runtimeClasspathMavenRepository) {
		CopySpec copySpec = project.copySpec();
		copySpec.from(project.getConfigurations().getByName(MavenRepositoryPlugin.MAVEN_REPOSITORY_CONFIGURATION_NAME));
		copySpec.from(new File(project.getBuildDir(), "maven-repository"));
		copySpec.from(runtimeClasspathMavenRepository);
		return copySpec;
	}

	private void addDocumentPluginGoalsTask(Project project, MavenExec generatePluginDescriptorTask) {
		DocumentPluginGoals task = project.getTasks().create("documentPluginGoals", DocumentPluginGoals.class);
		File pluginXml = new File(generatePluginDescriptorTask.getOutputs().getFiles().getSingleFile(), "plugin.xml");
		task.setPluginXml(pluginXml);
		task.setOutputDir(new File(project.getBuildDir(), "docs/generated/goals/"));
		task.dependsOn(generatePluginDescriptorTask);
	}

	private MavenExec addGenerateHelpMojoTask(Project project, Jar jarTask) {
		File helpMojoDir = new File(project.getBuildDir(), "help-mojo");
		MavenExec task = createGenerateHelpMojoTask(project, helpMojoDir);
		task.dependsOn(createSyncHelpMojoInputsTask(project, helpMojoDir));
		includeHelpMojoInJar(jarTask, task);
		return task;
	}

	private MavenExec createGenerateHelpMojoTask(Project project, File helpMojoDir) {
		MavenExec task = project.getTasks().create("generateHelpMojo", MavenExec.class);
		task.setProjectDir(helpMojoDir);
		task.args("org.apache.maven.plugins:maven-plugin-plugin:3.6.0:helpmojo");
		task.getOutputs().dir(new File(helpMojoDir, "target/generated-sources/plugin"));
		return task;
	}

	private Sync createSyncHelpMojoInputsTask(Project project, File helpMojoDir) {
		Sync task = project.getTasks().create("syncHelpMojoInputs", Sync.class);
		task.setDestinationDir(helpMojoDir);
		File pomFile = new File(project.getProjectDir(), "src/maven/resources/pom.xml");
		task.from(pomFile, (copy) -> replaceVersionPlaceholder(copy, project));
		return task;
	}

	private void includeHelpMojoInJar(Jar jarTask, JavaExec generateHelpMojoTask) {
		jarTask.from(generateHelpMojoTask).exclude("**/*.java");
		jarTask.dependsOn(generateHelpMojoTask);
	}

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

	private SourceSet getMainSourceSet(Project project) {
		SourceSetContainer sourceSets = project.getConvention().getPlugin(JavaPluginConvention.class).getSourceSets();
		return sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME);
	}

	private void setJavadocOptions(Javadoc javadoc) {
		StandardJavadocDocletOptions options = (StandardJavadocDocletOptions) javadoc.getOptions();
		options.addMultilineStringsOption("tag").setValue(Arrays.asList("goal:X", "requiresProject:X", "threadSafe:X"));
	}

	private FormatHelpMojoSource createFormatHelpMojoSource(Project project, MavenExec generateHelpMojoTask,
			File generatedHelpMojoDir) {
		FormatHelpMojoSource formatHelpMojoSource = project.getTasks().create("formatHelpMojoSource",
				FormatHelpMojoSource.class);
		formatHelpMojoSource.setGenerator(generateHelpMojoTask);
		formatHelpMojoSource.setOutputDir(generatedHelpMojoDir);
		return formatHelpMojoSource;
	}

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

	private MavenExec createGeneratePluginDescriptorTask(Project project, File mavenDir) {
		MavenExec generatePluginDescriptor = project.getTasks().create("generatePluginDescriptor", MavenExec.class);
		generatePluginDescriptor.args("org.apache.maven.plugins:maven-plugin-plugin:3.6.0:descriptor");
		generatePluginDescriptor.getOutputs().dir(new File(mavenDir, "target/classes/META-INF/maven"));
		generatePluginDescriptor.getInputs().dir(new File(mavenDir, "target/classes/org"))
				.withPathSensitivity(PathSensitivity.RELATIVE).withPropertyName("plugin classes");
		generatePluginDescriptor.setProjectDir(mavenDir);
		return generatePluginDescriptor;
	}

	private void includeDescriptorInJar(Jar jar, JavaExec generatePluginDescriptorTask) {
		jar.from(generatePluginDescriptorTask, (copy) -> copy.into("META-INF/maven/"));
		jar.dependsOn(generatePluginDescriptorTask);
	}

	private void addPrepareMavenBinariesTask(Project project) {
		PrepareMavenBinaries task = project.getTasks().create("prepareMavenBinaries", PrepareMavenBinaries.class);
		task.setOutputDir(new File(project.getBuildDir(), "maven-binaries"));
		project.getTasks().getByName(IntegrationTestPlugin.INT_TEST_TASK_NAME).dependsOn(task);
	}

	private void replaceVersionPlaceholder(CopySpec copy, Project project) {
		copy.filter((input) -> replaceVersionPlaceholder(project, input));
	}

	private String replaceVersionPlaceholder(Project project, String input) {
		return input.replace("{{version}}", project.getVersion().toString());
	}

	private void addExtractVersionPropertiesTask(Project project) {
		ExtractVersionProperties extractVersionProperties = project.getTasks().create("extractVersionProperties",
				ExtractVersionProperties.class);
		extractVersionProperties.setEffectiveBoms(project.getConfigurations().create("versionProperties"));
		extractVersionProperties.getDestination().set(project.getLayout().getBuildDirectory().dir("generated-resources")
				.map((dir) -> dir.file("extracted-versions.properties")));
	}

	public static class FormatHelpMojoSource extends DefaultTask {

		private Task generator;

		private File outputDir;

		void setGenerator(Task generator) {
			this.generator = generator;
			getInputs().files(this.generator).withPathSensitivity(PathSensitivity.RELATIVE)
					.withPropertyName("generated source");
		}

		@OutputDirectory
		public File getOutputDir() {
			return this.outputDir;
		}

		void setOutputDir(File outputDir) {
			this.outputDir = outputDir;
		}

		@TaskAction
		void syncAndFormat() {
			FileFormatter formatter = new FileFormatter(FORMATTER_CONFIG);
			for (File output : this.generator.getOutputs().getFiles()) {
				formatter.formatFiles(getProject().fileTree(output), StandardCharsets.UTF_8)
						.forEach((edit) -> save(output, edit));
			}
		}

		private void save(File output, FileEdit edit) {
			Path relativePath = output.toPath().relativize(edit.getFile().toPath());
			Path outputLocation = this.outputDir.toPath().resolve(relativePath);
			try {
				Files.createDirectories(outputLocation.getParent());
				Files.write(outputLocation, edit.getFormattedContent().getBytes(StandardCharsets.UTF_8));
			}
			catch (Exception ex) {
				throw new TaskExecutionException(this, ex);
			}
		}

	}

	public static class MavenRepositoryComponentMetadataRule implements ComponentMetadataRule {

		private final ObjectFactory objects;

		@javax.inject.Inject
		public MavenRepositoryComponentMetadataRule(ObjectFactory objects) {
			this.objects = objects;
		}

		@Override
		public void execute(ComponentMetadataContext context) {
			context.getDetails().maybeAddVariant("compileWithMetadata", "compile",
					(variant) -> configureVariant(context, variant));
			context.getDetails().maybeAddVariant("apiElementsWithMetadata", "apiElements",
					(variant) -> configureVariant(context, variant));
		}

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

	public static class RuntimeClasspathMavenRepository extends DefaultTask {

		private final Configuration runtimeClasspath;

		private final DirectoryProperty outputDirectory;

		public RuntimeClasspathMavenRepository() {
			this.runtimeClasspath = getProject().getConfigurations().getByName("runtimeClasspathWithMetadata");
			this.outputDirectory = getProject().getObjects().directoryProperty();
		}

		@OutputDirectory
		public DirectoryProperty getOutputDirectory() {
			return this.outputDirectory;
		}

		@Classpath
		public Configuration getRuntimeClasspath() {
			return this.runtimeClasspath;
		}

		@TaskAction
		public void createRepository() {
			for (ResolvedArtifactResult result : this.runtimeClasspath.getIncoming().getArtifacts()) {
				if (result.getId().getComponentIdentifier() instanceof ModuleComponentIdentifier) {
					ModuleComponentIdentifier identifier = (ModuleComponentIdentifier) result.getId()
							.getComponentIdentifier();
					String fileName = result.getFile().getName()
							.replace(identifier.getVersion() + "-" + identifier.getVersion(), identifier.getVersion());
					File repositoryLocation = this.outputDirectory.dir(identifier.getGroup().replace('.', '/') + "/"
							+ identifier.getModule() + "/" + identifier.getVersion() + "/" + fileName).get()
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

	public static class ExtractVersionProperties extends DefaultTask {

		private final RegularFileProperty destination;

		private FileCollection effectiveBoms;

		public ExtractVersionProperties() {
			this.destination = getProject().getObjects().fileProperty();
		}

		@InputFiles
		@PathSensitive(PathSensitivity.RELATIVE)
		public FileCollection getEffectiveBoms() {
			return this.effectiveBoms;
		}

		public void setEffectiveBoms(FileCollection effectiveBoms) {
			this.effectiveBoms = effectiveBoms;
		}

		@OutputFile
		public RegularFileProperty getDestination() {
			return this.destination;
		}

		@TaskAction
		public void extractVersionProperties() {
			EffectiveBom effectiveBom = new EffectiveBom(this.effectiveBoms.getSingleFile());
			Properties versions = extractVersionProperties(effectiveBom);
			writeProperties(versions);
		}

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
			return versions;
		}

	}

	private static final class EffectiveBom {

		private final Document document;

		private final XPath xpath;

		private EffectiveBom(File bomFile) {
			this.document = loadDocument(bomFile);
			this.xpath = XPathFactory.newInstance().newXPath();
		}

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

		private String version() {
			return get("version");
		}

		private void property(String name, BiConsumer<String, String> handler) {
			handler.accept(name, get("properties/" + name));
		}

		private String get(String expression) {
			try {
				Node node = (Node) this.xpath.compile("/project/" + expression).evaluate(this.document,
						XPathConstants.NODE);
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
