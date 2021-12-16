/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.build.starters;

import java.io.File;
import java.util.Map;
import java.util.TreeMap;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.plugins.JavaBasePlugin;
import org.gradle.api.plugins.JavaLibraryPlugin;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.PluginContainer;
import org.gradle.api.tasks.bundling.Jar;

import org.springframework.boot.build.ConventionsPlugin;
import org.springframework.boot.build.DeployedPlugin;
import org.springframework.boot.build.classpath.CheckClasspathForConflicts;
import org.springframework.boot.build.classpath.CheckClasspathForUnnecessaryExclusions;
import org.springframework.util.StringUtils;

/**
 * A {@link Plugin} for a starter project.
 *
 * @author Andy Wilkinson
 */
public class StarterPlugin implements Plugin<Project> {

	private static final String JAR_TYPE = "dependencies-starter";

	@Override
	public void apply(Project project) {
		PluginContainer plugins = project.getPlugins();
		plugins.apply(DeployedPlugin.class);
		plugins.apply(JavaLibraryPlugin.class);
		plugins.apply(ConventionsPlugin.class);
		StarterMetadata starterMetadata = project.getTasks().create("starterMetadata", StarterMetadata.class);
		ConfigurationContainer configurations = project.getConfigurations();
		Configuration runtimeClasspath = configurations.getByName(JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME);
		starterMetadata.setDependencies(runtimeClasspath);
		File destination = new File(project.getBuildDir(), "starter-metadata.properties");
		starterMetadata.setDestination(destination);
		configurations.create("starterMetadata");
		project.getArtifacts().add("starterMetadata", project.provider(starterMetadata::getDestination),
				(artifact) -> artifact.builtBy(starterMetadata));
		createClasspathConflictsCheck(runtimeClasspath, project);
		createUnnecessaryExclusionsCheck(runtimeClasspath, project);
		configureJarManifest(project);
	}

	private void createClasspathConflictsCheck(Configuration classpath, Project project) {
		CheckClasspathForConflicts checkClasspathForConflicts = project.getTasks().create(
				"check" + StringUtils.capitalize(classpath.getName() + "ForConflicts"),
				CheckClasspathForConflicts.class);
		checkClasspathForConflicts.setClasspath(classpath);
		project.getTasks().getByName(JavaBasePlugin.CHECK_TASK_NAME).dependsOn(checkClasspathForConflicts);
	}

	private void createUnnecessaryExclusionsCheck(Configuration classpath, Project project) {
		CheckClasspathForUnnecessaryExclusions checkClasspathForUnnecessaryExclusions = project.getTasks().create(
				"check" + StringUtils.capitalize(classpath.getName() + "ForUnnecessaryExclusions"),
				CheckClasspathForUnnecessaryExclusions.class);
		checkClasspathForUnnecessaryExclusions.setClasspath(classpath);
		project.getTasks().getByName(JavaBasePlugin.CHECK_TASK_NAME).dependsOn(checkClasspathForUnnecessaryExclusions);
	}

	private void configureJarManifest(Project project) {
		project.getTasks().withType(Jar.class, (jar) -> project.afterEvaluate((evaluated) -> {
			jar.manifest((manifest) -> {
				Map<String, Object> attributes = new TreeMap<>();
				attributes.put("Spring-Boot-Jar-Type", JAR_TYPE);
				manifest.attributes(attributes);
			});
		}));
	}

}
