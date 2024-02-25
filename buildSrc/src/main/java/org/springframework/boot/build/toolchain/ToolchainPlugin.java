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

package org.springframework.boot.build.toolchain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.testing.Test;
import org.gradle.jvm.toolchain.JavaLanguageVersion;
import org.gradle.jvm.toolchain.JavaToolchainSpec;

/**
 * {@link Plugin} for customizing Gradle's toolchain support.
 *
 * @author Christoph Dreis
 */
public class ToolchainPlugin implements Plugin<Project> {

	/**
     * Applies the toolchain configuration to the specified project.
     * 
     * @param project the project to apply the toolchain configuration to
     */
    @Override
	public void apply(Project project) {
		configureToolchain(project);
	}

	/**
     * Configures the toolchain for the given project.
     * 
     * @param project the project to configure the toolchain for
     */
    private void configureToolchain(Project project) {
		ToolchainExtension toolchain = project.getExtensions().create("toolchain", ToolchainExtension.class, project);
		JavaLanguageVersion toolchainVersion = toolchain.getJavaVersion();
		if (toolchainVersion != null) {
			project.afterEvaluate((evaluated) -> configure(evaluated, toolchain));
		}
	}

	/**
     * Configures the project with the given toolchain.
     * If the Java version specified in the toolchain is not supported, disables the toolchain tasks.
     * Otherwise, sets the language version of the Java toolchain to the specified Java version,
     * and configures the test toolchain for the project.
     *
     * @param project   the project to be configured
     * @param toolchain the toolchain to be used for configuration
     */
    private void configure(Project project, ToolchainExtension toolchain) {
		if (!isJavaVersionSupported(toolchain, toolchain.getJavaVersion())) {
			disableToolchainTasks(project);
		}
		else {
			JavaToolchainSpec toolchainSpec = project.getExtensions()
				.getByType(JavaPluginExtension.class)
				.getToolchain();
			toolchainSpec.getLanguageVersion().set(toolchain.getJavaVersion());
			configureTestToolchain(project, toolchain);
		}
	}

	/**
     * Checks if the given Java version is supported by the toolchain.
     * 
     * @param toolchain The toolchain extension.
     * @param toolchainVersion The Java language version of the toolchain.
     * @return {@code true} if the Java version is supported, {@code false} otherwise.
     */
    private boolean isJavaVersionSupported(ToolchainExtension toolchain, JavaLanguageVersion toolchainVersion) {
		return toolchain.getMaximumCompatibleJavaVersion()
			.map((version) -> version.canCompileOrRun(toolchainVersion))
			.getOrElse(true);
	}

	/**
     * Disables all toolchain tasks in the given project.
     * 
     * @param project the project in which to disable the toolchain tasks
     */
    private void disableToolchainTasks(Project project) {
		project.getTasks().withType(Test.class, (task) -> task.setEnabled(false));
	}

	/**
     * Configures the test toolchain for the given project.
     * 
     * @param project the project to configure the toolchain for
     * @param toolchain the toolchain extension to use for configuration
     */
    private void configureTestToolchain(Project project, ToolchainExtension toolchain) {
		List<String> jvmArgs = new ArrayList<>();
		jvmArgs.addAll(toolchain.getTestJvmArgs().getOrElse(Collections.emptyList()));
		project.getTasks().withType(Test.class, (test) -> test.jvmArgs(jvmArgs));
	}

}
