/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.launchscript;

import java.io.File;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import org.assertj.core.api.Condition;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.ToStringConsumer;
import org.testcontainers.containers.startupcheck.OneShotStartupCheckStrategy;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.utility.MountableFile;

import org.springframework.boot.ansi.AnsiColor;
import org.springframework.util.Assert;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;

/**
 * Abstract base class for testing the launch script.
 *
 * @author Andy Wilkinson
 * @author Ali Shahbour
 * @author Alexey Vinogradov
 */
abstract class AbstractLaunchScriptIntegrationTests {

	protected static final char ESC = 27;

	private final String scriptsDir;

	protected AbstractLaunchScriptIntegrationTests(String scriptsDir) {
		this.scriptsDir = scriptsDir;
	}

	static List<Object[]> parameters(Predicate<File> osFilter) {
		List<Object[]> parameters = new ArrayList<>();
		for (File os : new File("src/intTest/resources/conf").listFiles()) {
			if (osFilter.test(os)) {
				for (File version : os.listFiles()) {
					parameters.add(new Object[] { os.getName(), version.getName() });
				}
			}
		}
		return parameters;
	}

	protected Condition<String> coloredString(AnsiColor color, String string) {
		String colorString = ESC + "[0;" + color + "m" + string + ESC + "[0m";
		return new Condition<String>() {

			@Override
			public boolean matches(String value) {
				return containsString(colorString).matches(value);
			}

		};
	}

	protected void doLaunch(String os, String version, String script) throws Exception {
		assertThat(doTest(os, version, script)).contains("Launched");
	}

	protected String doTest(String os, String version, String script) throws Exception {
		ToStringConsumer consumer = new ToStringConsumer().withRemoveAnsiCodes(false);
		try (LaunchScriptTestContainer container = new LaunchScriptTestContainer(os, version, this.scriptsDir,
				script)) {
			container.withLogConsumer(consumer);
			container.start();
			while (container.isRunning()) {
				Thread.sleep(100);
			}
		}
		return consumer.toUtf8String();
	}

	private static final class LaunchScriptTestContainer extends GenericContainer<LaunchScriptTestContainer> {

		private LaunchScriptTestContainer(String os, String version, String scriptsDir, String testScript) {
			super(new ImageFromDockerfile("spring-boot-launch-script/" + os.toLowerCase() + "-" + version)
					.withFileFromFile("Dockerfile",
							new File("src/intTest/resources/conf/" + os + "/" + version + "/Dockerfile")));
			withCopyFileToContainer(MountableFile.forHostPath(findApplication().getAbsolutePath()), "/app.jar");
			withCopyFileToContainer(
					MountableFile.forHostPath("src/intTest/resources/scripts/" + scriptsDir + "test-functions.sh"),
					"/test-functions.sh");
			withCopyFileToContainer(
					MountableFile.forHostPath("src/intTest/resources/scripts/" + scriptsDir + testScript),
					"/" + testScript);
			withCommand("/bin/bash", "-c", "chmod +x " + testScript + " && ./" + testScript);
			withStartupCheckStrategy(new OneShotStartupCheckStrategy().withTimeout(Duration.ofMinutes(5)));
		}

		private static File findApplication() {
			String name = String.format("build/%1$s/build/libs/%1$s.jar", "spring-boot-launch-script-tests-app");
			File jar = new File(name);
			Assert.state(jar.isFile(), () -> "Could not find " + name + ". Have you built it?");
			return jar;
		}

	}

}
