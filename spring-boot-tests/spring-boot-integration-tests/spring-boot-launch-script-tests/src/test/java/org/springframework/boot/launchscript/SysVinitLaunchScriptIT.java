/*
 * Copyright 2012-2019 the original author or authors.
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
import java.util.regex.Pattern;

import org.assertj.core.api.Condition;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.ToStringConsumer;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.utility.MountableFile;

import org.springframework.boot.ansi.AnsiColor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assume.assumeThat;

/**
 * Integration tests for Spring Boot's launch script on OSs that use SysVinit.
 *
 * @author Andy Wilkinson
 * @author Ali Shahbour
 */
@RunWith(Parameterized.class)
public class SysVinitLaunchScriptIT {

	private static final char ESC = 27;

	private final String os;

	private final String version;

	@Parameters(name = "{0} {1}")
	public static List<Object[]> parameters() {
		List<Object[]> parameters = new ArrayList<>();
		for (File os : new File("src/test/resources/conf").listFiles()) {
			for (File version : os.listFiles()) {
				parameters.add(new Object[] { os.getName(), version.getName() });
			}
		}
		return parameters;
	}

	public SysVinitLaunchScriptIT(String os, String version) {
		this.os = os;
		this.version = version;
	}

	@Test
	public void statusWhenStopped() throws Exception {
		String output = doTest("status-when-stopped.sh");
		assertThat(output).contains("Status: 3");
		assertThat(output).has(coloredString(AnsiColor.RED, "Not running"));
	}

	@Test
	public void statusWhenStarted() throws Exception {
		String output = doTest("status-when-started.sh");
		assertThat(output).contains("Status: 0");
		assertThat(output).has(coloredString(AnsiColor.GREEN, "Started [" + extractPid(output) + "]"));
	}

	@Test
	public void statusWhenKilled() throws Exception {
		String output = doTest("status-when-killed.sh");
		assertThat(output).contains("Status: 1");
		assertThat(output)
				.has(coloredString(AnsiColor.RED, "Not running (process " + extractPid(output) + " not found)"));
	}

	@Test
	public void stopWhenStopped() throws Exception {
		String output = doTest("stop-when-stopped.sh");
		assertThat(output).contains("Status: 0");
		assertThat(output).has(coloredString(AnsiColor.YELLOW, "Not running (pidfile not found)"));
	}

	@Test
	public void forceStopWhenStopped() throws Exception {
		String output = doTest("force-stop-when-stopped.sh");
		assertThat(output).contains("Status: 0");
		assertThat(output).has(coloredString(AnsiColor.YELLOW, "Not running (pidfile not found)"));
	}

	@Test
	public void startWhenStarted() throws Exception {
		String output = doTest("start-when-started.sh");
		assertThat(output).contains("Status: 0");
		assertThat(output).has(coloredString(AnsiColor.YELLOW, "Already running [" + extractPid(output) + "]"));
	}

	@Test
	public void restartWhenStopped() throws Exception {
		String output = doTest("restart-when-stopped.sh");
		assertThat(output).contains("Status: 0");
		assertThat(output).has(coloredString(AnsiColor.YELLOW, "Not running (pidfile not found)"));
		assertThat(output).has(coloredString(AnsiColor.GREEN, "Started [" + extractPid(output) + "]"));
	}

	@Test
	public void restartWhenStarted() throws Exception {
		String output = doTest("restart-when-started.sh");
		assertThat(output).contains("Status: 0");
		assertThat(output).has(coloredString(AnsiColor.GREEN, "Started [" + extract("PID1", output) + "]"));
		assertThat(output).has(coloredString(AnsiColor.GREEN, "Stopped [" + extract("PID1", output) + "]"));
		assertThat(output).has(coloredString(AnsiColor.GREEN, "Started [" + extract("PID2", output) + "]"));
	}

	@Test
	public void startWhenStopped() throws Exception {
		String output = doTest("start-when-stopped.sh");
		assertThat(output).contains("Status: 0");
		assertThat(output).has(coloredString(AnsiColor.GREEN, "Started [" + extractPid(output) + "]"));
	}

	@Test
	public void basicLaunch() throws Exception {
		String output = doTest("basic-launch.sh");
		assertThat(output).doesNotContain("PID_FOLDER");
	}

	@Test
	public void launchWithMissingLogFolderGeneratesAWarning() throws Exception {
		String output = doTest("launch-with-missing-log-folder.sh");
		assertThat(output).has(
				coloredString(AnsiColor.YELLOW, "LOG_FOLDER /does/not/exist does not exist. Falling back to /tmp"));
	}

	@Test
	public void launchWithMissingPidFolderGeneratesAWarning() throws Exception {
		String output = doTest("launch-with-missing-pid-folder.sh");
		assertThat(output).has(
				coloredString(AnsiColor.YELLOW, "PID_FOLDER /does/not/exist does not exist. Falling back to /tmp"));
	}

	@Test
	public void launchWithSingleCommandLineArgument() throws Exception {
		doLaunch("launch-with-single-command-line-argument.sh");
	}

	@Test
	public void launchWithMultipleCommandLineArguments() throws Exception {
		doLaunch("launch-with-multiple-command-line-arguments.sh");
	}

	@Test
	public void launchWithSingleRunArg() throws Exception {
		doLaunch("launch-with-single-run-arg.sh");
	}

	@Test
	public void launchWithMultipleRunArgs() throws Exception {
		doLaunch("launch-with-multiple-run-args.sh");
	}

	@Test
	public void launchWithSingleJavaOpt() throws Exception {
		doLaunch("launch-with-single-java-opt.sh");
	}

	@Test
	public void launchWithDoubleLinkSingleJavaOpt() throws Exception {
		doLaunch("launch-with-double-link-single-java-opt.sh");
	}

	@Test
	public void launchWithMultipleJavaOpts() throws Exception {
		doLaunch("launch-with-multiple-java-opts.sh");
	}

	@Test
	public void launchWithUseOfStartStopDaemonDisabled() throws Exception {
		// CentOS doesn't have start-stop-daemon
		assumeThat(this.os, is(not("CentOS")));
		doLaunch("launch-with-use-of-start-stop-daemon-disabled.sh");
	}

	@Test
	public void launchWithRelativePidFolder() throws Exception {
		String output = doTest("launch-with-relative-pid-folder.sh");
		assertThat(output).has(coloredString(AnsiColor.GREEN, "Started [" + extractPid(output) + "]"));
		assertThat(output).has(coloredString(AnsiColor.GREEN, "Running [" + extractPid(output) + "]"));
		assertThat(output).has(coloredString(AnsiColor.GREEN, "Stopped [" + extractPid(output) + "]"));
	}

	@Test
	public void pidFolderOwnership() throws Exception {
		String output = doTest("pid-folder-ownership.sh");
		assertThat(output).contains("phil root");
	}

	@Test
	public void pidFileOwnership() throws Exception {
		String output = doTest("pid-file-ownership.sh");
		assertThat(output).contains("phil root");
	}

	@Test
	public void logFileOwnership() throws Exception {
		String output = doTest("log-file-ownership.sh");
		assertThat(output).contains("phil root");
	}

	@Test
	public void logFileOwnershipIsChangedWhenCreated() throws Exception {
		String output = doTest("log-file-ownership-is-changed-when-created.sh");
		assertThat(output).contains("andy root");
	}

	@Test
	public void logFileOwnershipIsUnchangedWhenExists() throws Exception {
		String output = doTest("log-file-ownership-is-unchanged-when-exists.sh");
		assertThat(output).contains("root root");
	}

	@Test
	public void launchWithRelativeLogFolder() throws Exception {
		String output = doTest("launch-with-relative-log-folder.sh");
		assertThat(output).contains("Log written");
	}

	private void doLaunch(String script) throws Exception {
		assertThat(doTest(script)).contains("Launched");
	}

	private String doTest(String script) throws Exception {
		ToStringConsumer consumer = new ToStringConsumer().withRemoveAnsiCodes(false);
		try (LaunchScriptTestContainer container = new LaunchScriptTestContainer(this.os, this.version, script)) {
			container.withLogConsumer(consumer);
			container.start();
			while (container.isRunning()) {
				Thread.sleep(100);
			}
		}
		return consumer.toUtf8String();
	}

	private Condition<String> coloredString(AnsiColor color, String string) {
		String colorString = ESC + "[0;" + color + "m" + string + ESC + "[0m";
		return new Condition<String>() {

			@Override
			public boolean matches(String value) {
				return containsString(colorString).matches(value);
			}

		};
	}

	private String extractPid(String output) {
		return extract("PID", output);
	}

	private String extract(String label, String output) {
		Pattern pattern = Pattern.compile(".*" + label + ": ([0-9]+).*", Pattern.DOTALL);
		java.util.regex.Matcher matcher = pattern.matcher(output);
		if (matcher.matches()) {
			return matcher.group(1);
		}
		throw new IllegalArgumentException("Failed to extract " + label + " from output: " + output);
	}

	private static final class LaunchScriptTestContainer extends GenericContainer<LaunchScriptTestContainer> {

		private LaunchScriptTestContainer(String os, String version, String testScript) {
			super(new ImageFromDockerfile("spring-boot-launch-script/" + os.toLowerCase() + "-" + version)
					.withFileFromFile("Dockerfile",
							new File("src/test/resources/conf/" + os + "/" + version + "/Dockerfile"))
					.withFileFromFile("spring-boot-launch-script-tests.jar", findApplication())
					.withFileFromFile("test-functions.sh", new File("src/test/resources/scripts/test-functions.sh")));
			withCopyFileToContainer(MountableFile.forHostPath("src/test/resources/scripts/" + testScript),
					"/" + testScript);
			withCommand("/bin/bash", "-c", "chmod +x " + testScript + " && ./" + testScript);
			withStartupTimeout(Duration.ofMinutes(5));
		}

		private static File findApplication() {
			File targetDir = new File("target");
			for (File file : targetDir.listFiles()) {
				if (file.getName().startsWith("spring-boot-launch-script-tests") && file.getName().endsWith(".jar")
						&& !file.getName().endsWith("-sources.jar")) {
					return file;
				}
			}
			throw new IllegalStateException(
					"Could not find test application in target directory. Have you built it (mvn package)?");
		}

	}

}
