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

package org.springframework.boot.build.toolchain;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.api.tasks.javadoc.Javadoc;
import org.gradle.api.tasks.testing.Test;
import org.gradle.jvm.toolchain.JavaLanguageVersion;
import org.gradle.jvm.toolchain.JavaToolchainService;
import org.gradle.jvm.toolchain.JavaToolchainSpec;

/**
 * {@link Plugin} for customizing Gradle's toolchain support.
 *
 * @author Christoph Dreis
 */
public class ToolchainPlugin implements Plugin<Project> {

	@Override
	public void apply(Project project) {
		configureToolchain(project);
	}

	private void configureToolchain(Project project) {
		ToolchainExtension toolchain = project.getExtensions().create("toolchain", ToolchainExtension.class, project);
		project.afterEvaluate((evaluated) -> {
			Optional<JavaLanguageVersion> toolchainVersion = toolchain.getToolchainVersion();
			if (toolchainVersion.isPresent()) {
				if (!toolchain.isJavaVersionSupported()) {
					disableToolchainTasks(project);
				}
				else {
					configureJavaCompileToolchain(project, toolchain);
					configureJavadocToolchain(project, toolchain);
					configureTestToolchain(project, toolchain);
				}
			}
		});
	}

	private void disableToolchainTasks(Project project) {
		project.getTasks().withType(JavaCompile.class, (task) -> task.setEnabled(false));
		project.getTasks().withType(Javadoc.class, (task) -> task.setEnabled(false));
		project.getTasks().withType(Test.class, (task) -> task.setEnabled(false));
	}

	private void configureJavaCompileToolchain(Project project, ToolchainExtension toolchain) {
		project.getTasks().withType(JavaCompile.class, (compile) -> {
			withOptionalJavaToolchain(toolchain).ifPresent((action) -> {
				JavaToolchainService service = getJavaToolchainService(project);
				compile.getJavaCompiler().set(service.compilerFor(action));
				compile.getOptions().setFork(true);
				// See https://github.com/gradle/gradle/issues/15538
				List<String> forkArgs = Arrays.asList("--add-opens",
						"jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED");
				compile.getOptions().getForkOptions().getJvmArgs().addAll(forkArgs);
			});
		});
	}

	private void configureJavadocToolchain(Project project, ToolchainExtension toolchain) {
		project.getTasks().withType(Javadoc.class, (javadoc) -> {
			withOptionalJavaToolchain(toolchain).ifPresent((action) -> {
				JavaToolchainService service = getJavaToolchainService(project);
				javadoc.getJavadocTool().set(service.javadocToolFor(action));
			});
		});
	}

	private void configureTestToolchain(Project project, ToolchainExtension toolchain) {
		project.getTasks().withType(Test.class, (test) -> {
			withOptionalJavaToolchain(toolchain).ifPresent((action) -> {
				JavaToolchainService service = getJavaToolchainService(project);
				test.getJavaLauncher().set(service.launcherFor(action));
				// See https://github.com/spring-projects/spring-ldap/issues/570
				List<String> arguments = Arrays.asList("--add-exports=java.naming/com.sun.jndi.ldap=ALL-UNNAMED",
						"--illegal-access=warn");
				test.jvmArgs(arguments);
			});
		});
	}

	private JavaToolchainService getJavaToolchainService(Project project) {
		return project.getExtensions().getByType(JavaToolchainService.class);
	}

	private Optional<Action<JavaToolchainSpec>> withOptionalJavaToolchain(ToolchainExtension toolchain) {
		return toolchain.getToolchainVersion().map((toolchainVersion) -> {
			Action<JavaToolchainSpec> action = (javaToolchainSpec) -> javaToolchainSpec.getLanguageVersion()
					.convention(toolchainVersion);
			return Optional.of(action);
		}).orElse(Optional.empty());
	}

}
