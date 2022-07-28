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

package org.springframework.boot.maven;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.toolchain.Toolchain;
import org.apache.maven.toolchain.ToolchainManager;

import org.springframework.boot.loader.tools.JavaExecutable;
import org.springframework.boot.loader.tools.RunProcess;

/**
 * Ease the execution of a Java process using Maven's toolchain support.
 *
 * @author Stephane Nicoll
 */
class JavaProcessExecutor {

	private static final int EXIT_CODE_SIGINT = 130;

	private final MavenSession mavenSession;

	private final ToolchainManager toolchainManager;

	private final Consumer<RunProcess> runProcessCustomizer;

	JavaProcessExecutor(MavenSession mavenSession, ToolchainManager toolchainManager) {
		this(mavenSession, toolchainManager, null);
	}

	private JavaProcessExecutor(MavenSession mavenSession, ToolchainManager toolchainManager,
			Consumer<RunProcess> runProcessCustomizer) {
		this.mavenSession = mavenSession;
		this.toolchainManager = toolchainManager;
		this.runProcessCustomizer = runProcessCustomizer;
	}

	JavaProcessExecutor withRunProcessCustomizer(Consumer<RunProcess> customizer) {
		Consumer<RunProcess> combinedCustomizer = (this.runProcessCustomizer != null)
				? this.runProcessCustomizer.andThen(customizer) : customizer;
		return new JavaProcessExecutor(this.mavenSession, this.toolchainManager, combinedCustomizer);
	}

	int run(File workingDirectory, List<String> args, Map<String, String> environmentVariables)
			throws MojoExecutionException {
		RunProcess runProcess = new RunProcess(workingDirectory, getJavaExecutable());
		try {
			int exitCode = runProcess.run(true, args, environmentVariables);
			if (!hasTerminatedSuccessfully(exitCode)) {
				throw new MojoExecutionException("Process terminated with exit code: " + exitCode);
			}
			return exitCode;
		}
		catch (IOException ex) {
			throw new MojoExecutionException("Process execution failed", ex);
		}
	}

	RunProcess runAsync(File workingDirectory, List<String> args, Map<String, String> environmentVariables)
			throws MojoExecutionException {
		try {
			RunProcess runProcess = new RunProcess(workingDirectory, getJavaExecutable());
			runProcess.run(false, args, environmentVariables);
			return runProcess;
		}
		catch (IOException ex) {
			throw new MojoExecutionException("Process execution failed", ex);
		}
	}

	private boolean hasTerminatedSuccessfully(int exitCode) {
		return (exitCode == 0 || exitCode == EXIT_CODE_SIGINT);
	}

	private String getJavaExecutable() {
		Toolchain toolchain = this.toolchainManager.getToolchainFromBuildContext("jdk", this.mavenSession);
		String javaExecutable = (toolchain != null) ? toolchain.findTool("java") : null;
		return (javaExecutable != null) ? javaExecutable : new JavaExecutable().toString();
	}

}
