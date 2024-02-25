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

package org.springframework.boot.gradle.plugin;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.ExtraPropertiesExtension;
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginWrapper;
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginWrapperKt;
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile;

/**
 * {@link PluginApplicationAction} that reacts to Kotlin's Gradle plugin being applied by
 * configuring a {@code kotlin.version} property to align the version used for dependency
 * management for Kotlin with the version of its plugin.
 *
 * @author Andy Wilkinson
 */
class KotlinPluginAction implements PluginApplicationAction {

	/**
     * Executes the action on the given project.
     *
     * @param project the project on which the action is executed
     */
    @Override
	public void execute(Project project) {
		ExtraPropertiesExtension extraProperties = project.getExtensions().getExtraProperties();
		if (!extraProperties.has("kotlin.version")) {
			String kotlinVersion = getKotlinVersion(project);
			extraProperties.set("kotlin.version", kotlinVersion);
		}
		enableJavaParametersOption(project);
	}

	/**
     * Returns the version of Kotlin plugin used in the given project.
     *
     * @param project the project to get the Kotlin plugin version from
     * @return the version of Kotlin plugin used in the project
     */
    private String getKotlinVersion(Project project) {
		return KotlinPluginWrapperKt.getKotlinPluginVersion(project);
	}

	/**
     * Enables the Java parameters option for the specified project.
     *
     * @param project the project to enable the Java parameters option for
     */
    private void enableJavaParametersOption(Project project) {
		project.getTasks()
			.withType(KotlinCompile.class)
			.configureEach((compile) -> compile.getKotlinOptions().setJavaParameters(true));
	}

	/**
     * Returns the class of the plugin associated with this action.
     *
     * @return the class of the plugin
     */
    @Override
	public Class<? extends Plugin<? extends Project>> getPluginClass() {
		return KotlinPluginWrapper.class;
	}

}
