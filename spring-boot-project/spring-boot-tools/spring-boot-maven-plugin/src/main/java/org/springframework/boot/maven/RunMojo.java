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
import java.util.List;
import java.util.Map;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
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
public class RunMojo extends AbstractRunMojo {

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
		}
		return jvmArguments;
	}

	@Override
	protected void run(JavaProcessExecutor processExecutor, File workingDirectory, List<String> args,
			Map<String, String> environmentVariables) throws MojoExecutionException, MojoFailureException {
		processExecutor.withRunProcessCustomizer(
				(runProcess) -> Runtime.getRuntime().addShutdownHook(new Thread(new RunProcessKiller(runProcess))))
				.run(workingDirectory, args, environmentVariables);
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
