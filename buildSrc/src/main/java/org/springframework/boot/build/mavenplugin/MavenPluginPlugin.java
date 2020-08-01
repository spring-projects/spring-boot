/*
 * Copyright 2019-2020 the original author or authors.
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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import io.spring.javaformat.formatter.FileEdit;
import io.spring.javaformat.formatter.FileFormatter;
import org.gradle.api.DefaultTask;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.file.CopySpec;
import org.gradle.api.plugins.JavaLibraryPlugin;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.publish.PublishingExtension;
import org.gradle.api.publish.maven.MavenPublication;
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin;
import org.gradle.api.tasks.Copy;
import org.gradle.api.tasks.JavaExec;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.TaskExecutionException;
import org.gradle.api.tasks.bundling.Jar;
import org.gradle.api.tasks.javadoc.Javadoc;
import org.gradle.external.javadoc.StandardJavadocDocletOptions;

import org.springframework.boot.build.DeployedPlugin;
import org.springframework.boot.build.MavenRepositoryPlugin;
import org.springframework.boot.build.test.IntegrationTestPlugin;

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
		MavenExec generateHelpMojoTask = addGenerateHelpMojoTask(project, jarTask);
		MavenExec generatePluginDescriptorTask = addGeneratePluginDescriptorTask(project, jarTask,
				generateHelpMojoTask);
		addDocumentPluginGoalsTask(project, generatePluginDescriptorTask);
		addPrepareMavenBinariesTask(project);
	}

	private void configurePomPackaging(Project project) {
		PublishingExtension publishing = project.getExtensions().getByType(PublishingExtension.class);
		publishing.getPublications().withType(MavenPublication.class, this::setPackaging);
	}

	private void setPackaging(MavenPublication mavenPublication) {
		mavenPublication.pom((pom) -> pom.setPackaging("maven-plugin"));
	}

	private void addPopulateIntTestMavenRepositoryTask(Project project) {
		Copy task = project.getTasks().create("populateIntTestMavenRepository", Copy.class);
		task.setDestinationDir(project.getBuildDir());
		task.into("int-test-maven-repository", (copy) -> copyIntTestMavenRepositoryFiles(project, copy));
		task.dependsOn(project.getTasks().getByName(MavenRepositoryPlugin.PUBLISH_TO_PROJECT_REPOSITORY_TASK_NAME));
		project.getTasks().getByName(IntegrationTestPlugin.INT_TEST_TASK_NAME).dependsOn(task);
	}

	private void copyIntTestMavenRepositoryFiles(Project project, CopySpec copy) {
		copy.from(project.getConfigurations().getByName(MavenRepositoryPlugin.MAVEN_REPOSITORY_CONFIGURATION_NAME));
		copy.from(new File(project.getBuildDir(), "maven-repository"));
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
		task.dependsOn(createCopyHelpMojoInputsTask(project, helpMojoDir));
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

	private Copy createCopyHelpMojoInputsTask(Project project, File helpMojoDir) {
		Copy task = project.getTasks().create("copyHelpMojoInputs", Copy.class);
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
		FormatHelpMojoSourceTask copyFormattedHelpMojoSourceTask = createCopyFormattedHelpMojoSourceTask(project,
				generateHelpMojoTask, generatedHelpMojoDir);
		project.getTasks().getByName(mainSourceSet.getCompileJavaTaskName()).dependsOn(copyFormattedHelpMojoSourceTask);
		mainSourceSet.java((javaSources) -> javaSources.srcDir(generatedHelpMojoDir));
		Copy pluginDescriptorInputs = createCopyPluginDescriptorInputs(project, pluginDescriptorDir, mainSourceSet);
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

	private FormatHelpMojoSourceTask createCopyFormattedHelpMojoSourceTask(Project project,
			MavenExec generateHelpMojoTask, File generatedHelpMojoDir) {
		FormatHelpMojoSourceTask copyFormattedHelpMojoSourceTask = project.getTasks()
				.create("copyFormattedHelpMojoSource", FormatHelpMojoSourceTask.class);
		copyFormattedHelpMojoSourceTask.setGenerator(generateHelpMojoTask);
		copyFormattedHelpMojoSourceTask.setOutputDir(generatedHelpMojoDir);
		return copyFormattedHelpMojoSourceTask;
	}

	private Copy createCopyPluginDescriptorInputs(Project project, File destination, SourceSet sourceSet) {
		Copy pluginDescriptorInputs = project.getTasks().create("copyPluginDescriptorInputs", Copy.class);
		pluginDescriptorInputs.setDestinationDir(destination);
		File pomFile = new File(project.getProjectDir(), "src/maven/resources/pom.xml");
		pluginDescriptorInputs.from(pomFile, (copy) -> replaceVersionPlaceholder(copy, project));
		pluginDescriptorInputs.from(sourceSet.getOutput().getClassesDirs(), (sync) -> sync.into("target/classes"));
		pluginDescriptorInputs.from(sourceSet.getAllJava().getSrcDirs(), (sync) -> sync.into("src/main/java"));
		return pluginDescriptorInputs;
	}

	private MavenExec createGeneratePluginDescriptorTask(Project project, File mavenDir) {
		MavenExec generatePluginDescriptor = project.getTasks().create("generatePluginDescriptor", MavenExec.class);
		generatePluginDescriptor.args("org.apache.maven.plugins:maven-plugin-plugin:3.6.0:descriptor");
		generatePluginDescriptor.getOutputs().dir(new File(mavenDir, "target/classes/META-INF/maven"));
		generatePluginDescriptor.getInputs().dir(new File(mavenDir, "target/classes/org"));
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

	public static class FormatHelpMojoSourceTask extends DefaultTask {

		private Task generator;

		private File outputDir;

		void setGenerator(Task generator) {
			this.generator = generator;
			getInputs().files(this.generator);
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
			FileFormatter formatter = new FileFormatter();
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

}
