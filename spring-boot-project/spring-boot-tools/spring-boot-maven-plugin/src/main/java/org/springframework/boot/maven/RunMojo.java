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
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import org.springframework.boot.loader.tools.RunProcess;

/**
 * Run an application in place.
 *
 * @author Phillip Webb
 * @author Dmytro Nosan
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 * @since 1.0.0
 */
@Mojo(name = "run", requiresProject = true, defaultPhase = LifecyclePhase.VALIDATE,
		requiresDependencyResolution = ResolutionScope.TEST)
@Execute(phase = LifecyclePhase.TEST_COMPILE)
public class RunMojo extends AbstractApplicationRunMojo {

	/**
	 * Whether the JVM's launch should be optimized.
	 * @since 2.2.0
	 */
	@Parameter(property = "spring-boot.run.optimizedLaunch", defaultValue = "true")
	private boolean optimizedLaunch;

	@Override
	protected RunArguments resolveJvmArguments() {
		RunArguments jvmArguments = super.resolveJvmArguments();
		if (this.optimizedLaunch) {
			jvmArguments.getArgs().addFirst("-XX:TieredStopAtLevel=1");
			if (!isJava13OrLater()) {
				jvmArguments.getArgs().addFirst("-Xverify:none");
			}
		}
		return jvmArguments;
	}

	private boolean isJava13OrLater() {
		for (Method method : String.class.getMethods()) {
			if (method.getName().equals("stripIndent")) {
				return true;
			}
		}
		return false;
	}

	@Override
	protected void run(File workingDirectory, List<String> args, Map<String, String> environmentVariables)
			throws MojoExecutionException {
		int exitCode = forkJvm(workingDirectory, args, environmentVariables);
		if (hasTerminatedSuccessfully(exitCode)) {
			return;
		}
		throw new MojoExecutionException("Application finished with exit code: " + exitCode);
	}

	private int forkJvm(File workingDirectory, List<String> args, Map<String, String> environmentVariables)
			throws MojoExecutionException {
		try {
			RunProcess runProcess = new RunProcess(workingDirectory, getJavaExecutable());
			Runtime.getRuntime().addShutdownHook(new Thread(new RunProcessKiller(runProcess)));
			return runProcess.run(true, args, environmentVariables);
		}
		catch (Exception ex) {
			throw new MojoExecutionException("Could not exec java", ex);
		}
	}

	private static final class RunProcessKiller implements Runnable {

		private final RunProcess runProcess;

		private RunProcessKiller(RunProcess runProcess) {
			this.runProcess = runProcess;
		}

		@Override
		public void run() {
			this.runProcess.kill();
		}

	}

}
