/*
 * Copyright 2012-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.gradle.plugin;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;

import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.file.FileCollection;
import org.gradle.api.internal.artifacts.publish.ArchivePublishArtifact;
import org.gradle.api.plugins.ApplicationPlugin;
import org.gradle.api.plugins.BasePlugin;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.compile.JavaCompile;

import org.springframework.boot.gradle.tasks.bundling.BootJar;
import org.springframework.boot.gradle.tasks.run.BootRun;
import org.springframework.util.StringUtils;

/**
 * {@link Action} that is executed in response to the {@link JavaPlugin} being applied.
 *
 * @author Andy Wilkinson
 */
final class JavaPluginAction implements PluginApplicationAction {

	private static final String PARAMETERS_COMPILER_ARG = "-parameters";

	private final SinglePublishedArtifact singlePublishedArtifact;

	JavaPluginAction(SinglePublishedArtifact singlePublishedArtifact) {
		this.singlePublishedArtifact = singlePublishedArtifact;
	}

	@Override
	public Class<? extends Plugin<? extends Project>> getPluginClass() {
		return JavaPlugin.class;
	}

	@Override
	public void execute(Project project) {
		disableJarTask(project);
		configureBuildTask(project);
		BootJar bootJar = configureBootJarTask(project);
		configureArtifactPublication(bootJar);
		configureBootRunTask(project);
		configureUtf8Encoding(project);
		configureParametersCompilerArg(project);
		configureAdditionalMetadataLocations(project);
	}

	private void disableJarTask(Project project) {
		project.getTasks().getByName(JavaPlugin.JAR_TASK_NAME).setEnabled(false);
	}

	private void configureBuildTask(Project project) {
		project.getTasks().getByName(BasePlugin.ASSEMBLE_TASK_NAME)
				.dependsOn(this.singlePublishedArtifact);
	}

	private BootJar configureBootJarTask(Project project) {
		BootJar bootJar = project.getTasks().create(SpringBootPlugin.BOOT_JAR_TASK_NAME,
				BootJar.class);
		bootJar.setDescription("Assembles an executable jar archive containing the main"
				+ " classes and their dependencies.");
		bootJar.setGroup(BasePlugin.BUILD_GROUP);
		bootJar.classpath((Callable<FileCollection>) () -> {
			JavaPluginConvention convention = project.getConvention()
					.getPlugin(JavaPluginConvention.class);
			SourceSet mainSourceSet = convention.getSourceSets()
					.getByName(SourceSet.MAIN_SOURCE_SET_NAME);
			return mainSourceSet.getRuntimeClasspath();
		});
		bootJar.conventionMapping("mainClassName",
				new MainClassConvention(project, bootJar::getClasspath));
		return bootJar;
	}

	private void configureArtifactPublication(BootJar bootJar) {
		ArchivePublishArtifact artifact = new ArchivePublishArtifact(bootJar);
		this.singlePublishedArtifact.addCandidate(artifact);
	}

	private void configureBootRunTask(Project project) {
		JavaPluginConvention javaConvention = project.getConvention()
				.getPlugin(JavaPluginConvention.class);
		BootRun run = project.getTasks().create("bootRun", BootRun.class);
		run.setDescription("Runs this project as a Spring Boot application.");
		run.setGroup(ApplicationPlugin.APPLICATION_GROUP);
		run.classpath(javaConvention.getSourceSets()
				.findByName(SourceSet.MAIN_SOURCE_SET_NAME).getRuntimeClasspath());
		run.getConventionMapping().map("jvmArgs", () -> {
			if (project.hasProperty("applicationDefaultJvmArgs")) {
				return project.property("applicationDefaultJvmArgs");
			}
			return Collections.emptyList();
		});
		run.conventionMapping("main",
				new MainClassConvention(project, run::getClasspath));
	}

	private void configureUtf8Encoding(Project project) {
		project.afterEvaluate((evaluated) -> evaluated.getTasks()
				.withType(JavaCompile.class, (compile) -> {
					if (compile.getOptions().getEncoding() == null) {
						compile.getOptions().setEncoding("UTF-8");
					}
				}));
	}

	private void configureParametersCompilerArg(Project project) {
		project.getTasks().withType(JavaCompile.class, (compile) -> {
			List<String> compilerArgs = compile.getOptions().getCompilerArgs();
			if (!compilerArgs.contains(PARAMETERS_COMPILER_ARG)) {
				compilerArgs.add(PARAMETERS_COMPILER_ARG);
			}
		});
	}

	private void configureAdditionalMetadataLocations(Project project) {
		project.afterEvaluate((evaluated) -> evaluated.getTasks()
				.withType(JavaCompile.class, this::configureAdditionalMetadataLocations));
	}

	private void configureAdditionalMetadataLocations(JavaCompile compile) {
		compile.doFirst((task) -> {
			if (hasConfigurationProcessorOnClasspath(compile)) {
				findMatchingSourceSet(compile).ifPresent((sourceSet) -> {
					configureAdditionalMetadataLocations(compile, sourceSet);
				});
			}
		});
	}

	private Optional<SourceSet> findMatchingSourceSet(JavaCompile compile) {
		return compile.getProject().getConvention().getPlugin(JavaPluginConvention.class)
				.getSourceSets().stream().filter((sourceSet) -> sourceSet
						.getCompileJavaTaskName().equals(compile.getName()))
				.findFirst();
	}

	private boolean hasConfigurationProcessorOnClasspath(JavaCompile compile) {
		Set<File> files = compile.getOptions().getAnnotationProcessorPath() != null
				? compile.getOptions().getAnnotationProcessorPath().getFiles()
				: compile.getClasspath().getFiles();
		return files.stream().map(File::getName).anyMatch(
				(name) -> name.startsWith("spring-boot-configuration-processor"));
	}

	private void configureAdditionalMetadataLocations(JavaCompile compile,
			SourceSet sourceSet) {
		String locations = StringUtils
				.collectionToCommaDelimitedString(sourceSet.getResources().getSrcDirs());
		compile.getOptions().getCompilerArgs().add(
				"-Aorg.springframework.boot.configurationprocessor.additionalMetadataLocations="
						+ locations);
	}

}
