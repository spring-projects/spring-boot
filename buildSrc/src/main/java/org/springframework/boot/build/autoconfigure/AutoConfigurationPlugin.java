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

package org.springframework.boot.build.autoconfigure;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.SourceSet;

import org.springframework.boot.build.DeployedPlugin;
import org.springframework.boot.build.architecture.ArchitectureCheck;
import org.springframework.boot.build.architecture.ArchitecturePlugin;

/**
 * {@link Plugin} for projects that define auto-configuration. When applied, the plugin
 * applies the {@link DeployedPlugin}. Additionally, when the {@link JavaPlugin} is
 * applied it:
 *
 * <ul>
 * <li>Adds a dependency on the auto-configuration annotation processor.
 * <li>Defines a task that produces metadata describing the auto-configuration. The
 * metadata is made available as an artifact in the {@code autoConfigurationMetadata}
 * configuration.
 * <li>Reacts to the {@link ArchitecturePlugin} being applied and:
 * <ul>
 * <li>Adds a rule to the {@code checkArchitectureMain} task to verify that all
 * {@code AutoConfiguration} classes are listed in the {@code AutoConfiguration.imports}
 * file.
 * </ul>
 * </ul>
 *
 * @author Andy Wilkinson
 */
public class AutoConfigurationPlugin implements Plugin<Project> {

	/**
	 * Name of the {@link Configuration} that holds the auto-configuration metadata
	 * artifact.
	 */
	public static final String AUTO_CONFIGURATION_METADATA_CONFIGURATION_NAME = "autoConfigurationMetadata";

	private static final String AUTO_CONFIGURATION_IMPORTS_PATH = "META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports";

	@Override
	public void apply(Project project) {
		project.getPlugins().apply(DeployedPlugin.class);
		project.getPlugins().withType(JavaPlugin.class, (javaPlugin) -> {
			Configuration annotationProcessors = project.getConfigurations()
				.getByName(JavaPlugin.ANNOTATION_PROCESSOR_CONFIGURATION_NAME);
			annotationProcessors.getDependencies()
				.add(project.getDependencies()
					.project(Collections.singletonMap("path",
							":spring-boot-project:spring-boot-tools:spring-boot-autoconfigure-processor")));
			annotationProcessors.getDependencies()
				.add(project.getDependencies()
					.project(Collections.singletonMap("path",
							":spring-boot-project:spring-boot-tools:spring-boot-configuration-processor")));
			project.getTasks().create("autoConfigurationMetadata", AutoConfigurationMetadata.class, (task) -> {
				SourceSet main = project.getExtensions()
					.getByType(JavaPluginExtension.class)
					.getSourceSets()
					.getByName(SourceSet.MAIN_SOURCE_SET_NAME);
				task.setSourceSet(main);
				task.dependsOn(main.getClassesTaskName());
				task.setOutputFile(new File(project.getBuildDir(), "auto-configuration-metadata.properties"));
				project.getArtifacts()
					.add(AutoConfigurationPlugin.AUTO_CONFIGURATION_METADATA_CONFIGURATION_NAME,
							project.provider((Callable<File>) task::getOutputFile),
							(artifact) -> artifact.builtBy(task));
			});
			project.getPlugins().withType(ArchitecturePlugin.class, (architecturePlugin) -> {
				project.getTasks().named("checkArchitectureMain", ArchitectureCheck.class).configure((task) -> {
					SourceSet main = project.getExtensions()
						.getByType(JavaPluginExtension.class)
						.getSourceSets()
						.getByName(SourceSet.MAIN_SOURCE_SET_NAME);
					File resourcesDirectory = main.getOutput().getResourcesDir();
					task.dependsOn(main.getProcessResourcesTaskName());
					task.getInputs().files(resourcesDirectory).optional().withPathSensitivity(PathSensitivity.RELATIVE);
					task.getRules()
						.add(allClassesAnnotatedWithAutoConfigurationShouldBeListedInAutoConfigurationImports(
								autoConfigurationImports(project, resourcesDirectory)));
				});
			});
		});
	}

	private ArchRule allClassesAnnotatedWithAutoConfigurationShouldBeListedInAutoConfigurationImports(
			Provider<AutoConfigurationImports> imports) {
		return ArchRuleDefinition.classes()
			.that()
			.areAnnotatedWith("org.springframework.boot.autoconfigure.AutoConfiguration")
			.should(beListedInAutoConfigurationImports(imports))
			.allowEmptyShould(true);
	}

	private ArchCondition<JavaClass> beListedInAutoConfigurationImports(Provider<AutoConfigurationImports> imports) {
		return new ArchCondition<>("be listed in " + AUTO_CONFIGURATION_IMPORTS_PATH) {

			@Override
			public void check(JavaClass item, ConditionEvents events) {
				AutoConfigurationImports autoConfigurationImports = imports.get();
				if (!autoConfigurationImports.imports.contains(item.getName())) {
					events.add(SimpleConditionEvent.violated(item,
							item.getName() + " was not listed in " + autoConfigurationImports.importsFile));
				}
			}

		};
	}

	private Provider<AutoConfigurationImports> autoConfigurationImports(Project project, File resourcesDirectory) {
		Path importsFile = new File(resourcesDirectory, AUTO_CONFIGURATION_IMPORTS_PATH).toPath();
		return project.provider(() -> {
			try {
				return new AutoConfigurationImports(project.getProjectDir().toPath().relativize(importsFile),
						Files.readAllLines(importsFile));
			}
			catch (IOException ex) {
				throw new RuntimeException("Failed to read AutoConfiguration.imports", ex);
			}
		});
	}

	private static record AutoConfigurationImports(Path importsFile, List<String> imports) {

	}

}
