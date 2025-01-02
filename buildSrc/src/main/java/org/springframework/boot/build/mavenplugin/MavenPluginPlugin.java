/*
 * Copyright 2012-2025 the original author or authors.
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

import javax.inject.Inject;
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
import org.gradle.api.artifacts.ComponentMetadataContext;
import org.gradle.api.artifacts.ComponentMetadataRule;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ModuleVersionIdentifier;
import org.gradle.api.artifacts.VariantMetadata;
import org.gradle.api.artifacts.component.ModuleComponentIdentifier;
import org.gradle.api.artifacts.result.ResolvedArtifactResult;
import org.gradle.api.attributes.DocsType;
import org.gradle.api.attributes.Usage;
import org.gradle.api.component.AdhocComponentWithVariants;
import org.gradle.api.component.ConfigurationVariantDetails;
import org.gradle.api.component.SoftwareComponent;
import org.gradle.api.file.CopySpec;
import org.gradle.api.file.Directory;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.file.RegularFile;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.plugins.JavaLibraryPlugin;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.provider.Provider;
import org.gradle.api.publish.PublishingExtension;
import org.gradle.api.publish.maven.MavenPublication;
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin;
import org.gradle.api.publish.tasks.GenerateModuleMetadata;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.InputFiles;
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
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import org.springframework.boot.build.DeployedPlugin;
import org.springframework.boot.build.MavenRepositoryPlugin;
import org.springframework.boot.build.optional.OptionalDependenciesPlugin;
import org.springframework.boot.build.test.DockerTestPlugin;
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
		TaskProvider<MavenExec> generateHelpMojoTask = addGenerateHelpMojoTask(project, jarTask);
		TaskProvider<MavenExec> generatePluginDescriptorTask = addGeneratePluginDescriptorTask(project, jarTask,
				generateHelpMojoTask);
		addDocumentPluginGoalsTask(project, generatePluginDescriptorTask);
		addPrepareMavenBinariesTask(project);
		addExtractVersionPropertiesTask(project);
		publishOptionalDependenciesInPom(project);
		project.getTasks().withType(GenerateModuleMetadata.class).configureEach((task) -> task.setEnabled(false));
	}

	private void publishOptionalDependenciesInPom(Project project) {
		project.getPlugins().withType(OptionalDependenciesPlugin.class, (optionalDependencies) -> {
			SoftwareComponent component = project.getComponents().findByName("java");
			if (component instanceof AdhocComponentWithVariants componentWithVariants) {
				componentWithVariants.addVariantsFromConfiguration(
						project.getConfigurations().getByName(OptionalDependenciesPlugin.OPTIONAL_CONFIGURATION_NAME),
						ConfigurationVariantDetails::mapToOptional);
			}
		});
		MavenPublication publication = (MavenPublication) project.getExtensions()
			.getByType(PublishingExtension.class)
			.getPublications()
			.getByName("maven");
		publication.getPom().withXml((xml) -> {
			Element root = xml.asElement();
			NodeList children = root.getChildNodes();
			for (int i = 0; i < children.getLength(); i++) {
				Node child = children.item(i);
				if ("dependencyManagement".equals(child.getNodeName())) {
					root.removeChild(child);
				}
			}
		});
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
		TaskProvider<RuntimeClasspathMavenRepository> runtimeClasspathMavenRepository = project.getTasks()
			.register("runtimeClasspathMavenRepository", RuntimeClasspathMavenRepository.class,
					(task) -> task.getOutputDir()
						.set(project.getLayout().getBuildDirectory().dir("runtime-classpath-repository")));
		project.getDependencies()
			.components((components) -> components.all(MavenRepositoryComponentMetadataRule.class));
		TaskProvider<Sync> populateRepository = project.getTasks()
			.register("populateTestMavenRepository", Sync.class, (task) -> {
				task.setDestinationDir(
						project.getLayout().getBuildDirectory().dir("test-maven-repository").get().getAsFile());
				task.with(copyIntTestMavenRepositoryFiles(project, runtimeClasspathMavenRepository));
				task.dependsOn(
						project.getTasks().getByName(MavenRepositoryPlugin.PUBLISH_TO_PROJECT_REPOSITORY_TASK_NAME));
			});
		project.getTasks().getByName(IntegrationTestPlugin.INT_TEST_TASK_NAME).dependsOn(populateRepository);
		project.getPlugins()
			.withType(DockerTestPlugin.class)
			.all((dockerTestPlugin) -> project.getTasks()
				.named(DockerTestPlugin.DOCKER_TEST_TASK_NAME,
						(dockerTest) -> dockerTest.dependsOn(populateRepository)));
	}

	private CopySpec copyIntTestMavenRepositoryFiles(Project project,
			TaskProvider<RuntimeClasspathMavenRepository> runtimeClasspathMavenRepository) {
		CopySpec copySpec = project.copySpec();
		copySpec.from(project.getConfigurations().getByName(MavenRepositoryPlugin.MAVEN_REPOSITORY_CONFIGURATION_NAME));
		copySpec.from(project.getLayout().getBuildDirectory().dir("maven-repository"));
		copySpec.from(runtimeClasspathMavenRepository);
		return copySpec;
	}

	private void addDocumentPluginGoalsTask(Project project, TaskProvider<MavenExec> generatePluginDescriptorTask) {
		project.getTasks().register("documentPluginGoals", DocumentPluginGoals.class, (task) -> {
			ProjectLayout layout = project.getLayout();
			Provider<RegularFile> pluginXml = layout.file(generatePluginDescriptorTask
				.map((generateDescriptor) -> new File(generateDescriptor.getOutputs().getFiles().getSingleFile(),
						"plugin.xml")));
			task.getPluginXml().set(pluginXml);
			task.getOutputDir().set(layout.getBuildDirectory().dir("docs/generated/goals/"));
			task.dependsOn(generatePluginDescriptorTask);
		});
	}

	private TaskProvider<MavenExec> addGenerateHelpMojoTask(Project project, Jar jarTask) {
		Provider<Directory> helpMojoDir = project.getLayout().getBuildDirectory().dir("help-mojo");
		TaskProvider<Sync> syncHelpMojoInputs = createSyncHelpMojoInputsTask(project, helpMojoDir);
		TaskProvider<MavenExec> task = createGenerateHelpMojoTask(project, helpMojoDir, syncHelpMojoInputs);
		includeHelpMojoInJar(jarTask, task);
		return task;
	}

	private TaskProvider<MavenExec> createGenerateHelpMojoTask(Project project, Provider<Directory> helpMojoDir,
			TaskProvider<Sync> syncHelpMojoInputs) {
		return project.getTasks().register("generateHelpMojo", MavenExec.class, (task) -> {
			task.getProjectDir().set(helpMojoDir);
			task.args("org.apache.maven.plugins:maven-plugin-plugin:3.6.1:helpmojo");
			task.getOutputs().dir(helpMojoDir.map((directory) -> directory.dir("target/generated-sources/plugin")));
			task.dependsOn(syncHelpMojoInputs);
		});
	}

	private TaskProvider<Sync> createSyncHelpMojoInputsTask(Project project, Provider<Directory> helpMojoDir) {
		return project.getTasks().register("syncHelpMojoInputs", Sync.class, (task) -> {
			task.setDestinationDir(helpMojoDir.get().getAsFile());
			File pomFile = new File(project.getProjectDir(), "src/maven/resources/pom.xml");
			task.from(pomFile, (copy) -> replaceVersionPlaceholder(copy, project));
		});
	}

	private void includeHelpMojoInJar(Jar jarTask, TaskProvider<MavenExec> generateHelpMojoTask) {
		jarTask.from(generateHelpMojoTask).exclude("**/*.java");
		jarTask.dependsOn(generateHelpMojoTask);
	}

	private TaskProvider<MavenExec> addGeneratePluginDescriptorTask(Project project, Jar jarTask,
			TaskProvider<MavenExec> generateHelpMojoTask) {
		Provider<Directory> pluginDescriptorDir = project.getLayout().getBuildDirectory().dir("plugin-descriptor");
		Provider<Directory> generatedHelpMojoDir = project.getLayout()
			.getBuildDirectory()
			.dir("generated/sources/helpMojo");
		SourceSet mainSourceSet = getMainSourceSet(project);
		project.getTasks().withType(Javadoc.class, this::setJavadocOptions);
		TaskProvider<FormatHelpMojoSource> formattedHelpMojoSource = createFormatHelpMojoSource(project,
				generateHelpMojoTask, generatedHelpMojoDir);
		project.getTasks().getByName(mainSourceSet.getCompileJavaTaskName()).dependsOn(formattedHelpMojoSource);
		mainSourceSet.java((javaSources) -> javaSources.srcDir(formattedHelpMojoSource));
		TaskProvider<Sync> pluginDescriptorInputs = createSyncPluginDescriptorInputs(project, pluginDescriptorDir,
				mainSourceSet);
		TaskProvider<MavenExec> task = createGeneratePluginDescriptorTask(project, pluginDescriptorDir,
				pluginDescriptorInputs);
		includeDescriptorInJar(jarTask, task);
		return task;
	}

	private SourceSet getMainSourceSet(Project project) {
		SourceSetContainer sourceSets = project.getExtensions().getByType(JavaPluginExtension.class).getSourceSets();
		return sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME);
	}

	private void setJavadocOptions(Javadoc javadoc) {
		StandardJavadocDocletOptions options = (StandardJavadocDocletOptions) javadoc.getOptions();
		options.addMultilineStringsOption("tag").setValue(Arrays.asList("goal:X", "requiresProject:X", "threadSafe:X"));
	}

	private TaskProvider<FormatHelpMojoSource> createFormatHelpMojoSource(Project project,
			TaskProvider<MavenExec> generateHelpMojoTask, Provider<Directory> generatedHelpMojoDir) {
		return project.getTasks().register("formatHelpMojoSource", FormatHelpMojoSource.class, (task) -> {
			task.setGenerator(generateHelpMojoTask);
			task.getOutputDir().set(generatedHelpMojoDir);
		});
	}

	private TaskProvider<Sync> createSyncPluginDescriptorInputs(Project project, Provider<Directory> destination,
			SourceSet sourceSet) {
		return project.getTasks().register("syncPluginDescriptorInputs", Sync.class, (task) -> {
			task.setDestinationDir(destination.get().getAsFile());
			File pomFile = new File(project.getProjectDir(), "src/maven/resources/pom.xml");
			task.from(pomFile, (copy) -> replaceVersionPlaceholder(copy, project));
			task.from(sourceSet.getOutput().getClassesDirs(), (sync) -> sync.into("target/classes"));
			task.from(sourceSet.getAllJava().getSrcDirs(), (sync) -> sync.into("src/main/java"));
			task.getInputs().property("version", project.getVersion());
			task.dependsOn(sourceSet.getClassesTaskName());
		});
	}

	private TaskProvider<MavenExec> createGeneratePluginDescriptorTask(Project project, Provider<Directory> mavenDir,
			TaskProvider<Sync> pluginDescriptorInputs) {
		return project.getTasks().register("generatePluginDescriptor", MavenExec.class, (task) -> {
			task.args("org.apache.maven.plugins:maven-plugin-plugin:3.6.1:descriptor");
			task.getOutputs().dir(mavenDir.map((directory) -> directory.dir("target/classes/META-INF/maven")));
			task.getInputs()
				.dir(mavenDir.map((directory) -> directory.dir("target/classes/org")))
				.withPathSensitivity(PathSensitivity.RELATIVE)
				.withPropertyName("plugin classes");
			task.getProjectDir().set(mavenDir);
			task.dependsOn(pluginDescriptorInputs);
		});
	}

	private void includeDescriptorInJar(Jar jar, TaskProvider<MavenExec> generatePluginDescriptorTask) {
		jar.from(generatePluginDescriptorTask, (copy) -> copy.into("META-INF/maven/"));
		jar.dependsOn(generatePluginDescriptorTask);
	}

	private void addPrepareMavenBinariesTask(Project project) {
		TaskProvider<PrepareMavenBinaries> task = project.getTasks()
			.register("prepareMavenBinaries", PrepareMavenBinaries.class,
					(prepareMavenBinaries) -> prepareMavenBinaries.getOutputDir()
						.set(project.getLayout().getBuildDirectory().dir("maven-binaries")));
		project.getTasks()
			.getByName(IntegrationTestPlugin.INT_TEST_TASK_NAME)
			.getInputs()
			.dir(task.map(PrepareMavenBinaries::getOutputDir))
			.withPathSensitivity(PathSensitivity.RELATIVE)
			.withPropertyName("mavenBinaries");
	}

	private void replaceVersionPlaceholder(CopySpec copy, Project project) {
		copy.filter((input) -> replaceVersionPlaceholder(project, input));
	}

	private String replaceVersionPlaceholder(Project project, String input) {
		return input.replace("{{version}}", project.getVersion().toString());
	}

	private void addExtractVersionPropertiesTask(Project project) {
		project.getTasks().register("extractVersionProperties", ExtractVersionProperties.class, (task) -> {
			task.setEffectiveBoms(project.getConfigurations().create("versionProperties"));
			task.getDestination()
				.set(project.getLayout()
					.getBuildDirectory()
					.dir("generated-resources")
					.map((dir) -> dir.file("extracted-versions.properties")));
		});
	}

	public abstract static class FormatHelpMojoSource extends DefaultTask {

		private final ObjectFactory objectFactory;

		@Inject
		public FormatHelpMojoSource(ObjectFactory objectFactory) {
			this.objectFactory = objectFactory;
		}

		private TaskProvider<?> generator;

		void setGenerator(TaskProvider<?> generator) {
			this.generator = generator;
			getInputs().files(this.generator)
				.withPathSensitivity(PathSensitivity.RELATIVE)
				.withPropertyName("generated source");
		}

		@OutputDirectory
		public abstract DirectoryProperty getOutputDir();

		@TaskAction
		void syncAndFormat() {
			FileFormatter formatter = new FileFormatter();
			for (File output : this.generator.get().getOutputs().getFiles()) {
				formatter.formatFiles(this.objectFactory.fileTree().from(output), StandardCharsets.UTF_8)
					.forEach((edit) -> save(output, edit));
			}
		}

		private void save(File output, FileEdit edit) {
			Path relativePath = output.toPath().relativize(edit.getFile().toPath());
			Path outputLocation = getOutputDir().getAsFile().get().toPath().resolve(relativePath);
			try {
				Files.createDirectories(outputLocation.getParent());
				Files.writeString(outputLocation, edit.getFormattedContent());
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
			context.getDetails()
				.maybeAddVariant("compileWithMetadata", "compile", (variant) -> configureVariant(context, variant));
			context.getDetails()
				.maybeAddVariant("apiElementsWithMetadata", "apiElements",
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

	public abstract static class RuntimeClasspathMavenRepository extends DefaultTask {

		private final Configuration runtimeClasspath;

		public RuntimeClasspathMavenRepository() {
			this.runtimeClasspath = getProject().getConfigurations().getByName("runtimeClasspathWithMetadata");
		}

		@OutputDirectory
		public abstract DirectoryProperty getOutputDir();

		@Classpath
		public Configuration getRuntimeClasspath() {
			return this.runtimeClasspath;
		}

		@TaskAction
		public void createRepository() {
			for (ResolvedArtifactResult result : this.runtimeClasspath.getIncoming().getArtifacts()) {
				if (result.getId().getComponentIdentifier() instanceof ModuleComponentIdentifier identifier) {
					String fileName = result.getFile()
						.getName()
						.replace(identifier.getVersion() + "-" + identifier.getVersion(), identifier.getVersion());
					File repositoryLocation = getOutputDir()
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

	public abstract static class ExtractVersionProperties extends DefaultTask {

		private FileCollection effectiveBoms;

		@InputFiles
		@PathSensitive(PathSensitivity.RELATIVE)
		public FileCollection getEffectiveBoms() {
			return this.effectiveBoms;
		}

		public void setEffectiveBoms(FileCollection effectiveBoms) {
			this.effectiveBoms = effectiveBoms;
		}

		@OutputFile
		public abstract RegularFileProperty getDestination();

		@TaskAction
		public void extractVersionProperties() {
			EffectiveBom effectiveBom = new EffectiveBom(this.effectiveBoms.getSingleFile());
			Properties versions = extractVersionProperties(effectiveBom);
			writeProperties(versions);
		}

		private void writeProperties(Properties versions) {
			File outputFile = getDestination().getAsFile().get();
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
			effectiveBom.property("assertj.version", versions::setProperty);
			effectiveBom.property("junit-jupiter.version", versions::setProperty);
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
