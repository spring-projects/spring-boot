/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.build.toolchain;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.tasks.testing.Test;
import org.gradle.jvm.toolchain.JavaLanguageVersion;
import org.gradle.jvm.toolchain.JavaToolchainService;

/**
 * {@link Plugin} for customizing Gradle's toolchain support.
 *
 * @author Christoph Dreis
 * @author Andy Wilkinson
 */
public class ToolchainPlugin implements Plugin<Project> {

	@Override
	public void apply(Project project) {
		configureToolchain(project);
	}

	private void configureToolchain(Project project) {
		ToolchainExtension toolchain = project.getExtensions().create("toolchain", ToolchainExtension.class, project);
		JavaLanguageVersion toolchainVersion = toolchain.getJavaVersion();
		if (toolchainVersion != null) {
			project.afterEvaluate((evaluated) -> configure(evaluated, toolchain));
		}
	}

	private void configure(Project project, ToolchainExtension toolchain) {
		if (!isJavaVersionSupported(toolchain, toolchain.getJavaVersion())) {
			disableToolchainTasks(project);
		}
		else {
			configureTestToolchain(project, toolchain.getJavaVersion());
		}
	}

	private boolean isJavaVersionSupported(ToolchainExtension toolchain, JavaLanguageVersion toolchainVersion) {
		JavaLanguageVersion minimumVersion = toolchain.getMinimumCompatibleJavaVersion().getOrNull();
		if (minimumVersion == null || toolchainVersion.canCompileOrRun(minimumVersion)) {
			return toolchain.getMaximumCompatibleJavaVersion()
				.map((version) -> version.canCompileOrRun(toolchainVersion))
				.getOrElse(true);
		}
		return false;
	}

	private void disableToolchainTasks(Project project) {
		project.getTasks().withType(Test.class, (task) -> task.setEnabled(false));
	}

	private void configureTestToolchain(Project project, JavaLanguageVersion toolchainVersion) {
		JavaToolchainService javaToolchains = project.getExtensions().getByType(JavaToolchainService.class);
		project.getTasks()
			.withType(Test.class, (test) -> test.getJavaLauncher()
				.set(javaToolchains.launcherFor((spec) -> spec.getLanguageVersion().set(toolchainVersion))));
	}

}
