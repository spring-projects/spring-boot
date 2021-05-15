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

import java.util.List;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import org.springframework.boot.ansi.AnsiColor;
import org.springframework.boot.testsupport.testcontainers.DisabledIfDockerUnavailable;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for Spring Boot's launch script on OSs that use SysVinit.
 *
 * @author Andy Wilkinson
 * @author Ali Shahbour
 * @author Alexey Vinogradov
 */
@DisabledIfDockerUnavailable
class SysVinitLaunchScriptIntegrationTests extends AbstractLaunchScriptIntegrationTests {

	SysVinitLaunchScriptIntegrationTests() {
		super("init.d/");
	}

	static List<Object[]> parameters() {
		return parameters((file) -> !file.getName().contains("CentOS"));
	}

	@ParameterizedTest(name = "{0} {1}")
	@MethodSource("parameters")
	void statusWhenStopped(String os, String version) throws Exception {
		String output = doTest(os, version, "status-when-stopped.sh");
		assertThat(output).contains("Status: 3");
		assertThat(output).has(coloredString(AnsiColor.RED, "Not running"));
	}

	@ParameterizedTest(name = "{0} {1}")
	@MethodSource("parameters")
	void statusWhenStarted(String os, String version) throws Exception {
		String output = doTest(os, version, "status-when-started.sh");
		assertThat(output).contains("Status: 0");
		assertThat(output).has(coloredString(AnsiColor.GREEN, "Started [" + extractPid(output) + "]"));
	}

	@ParameterizedTest(name = "{0} {1}")
	@MethodSource("parameters")
	void statusWhenKilled(String os, String version) throws Exception {
		String output = doTest(os, version, "status-when-killed.sh");
		assertThat(output).contains("Status: 1");
		assertThat(output)
				.has(coloredString(AnsiColor.RED, "Not running (process " + extractPid(output) + " not found)"));
	}

	@ParameterizedTest(name = "{0} {1}")
	@MethodSource("parameters")
	void stopWhenStopped(String os, String version) throws Exception {
		String output = doTest(os, version, "stop-when-stopped.sh");
		assertThat(output).contains("Status: 0");
		assertThat(output).has(coloredString(AnsiColor.YELLOW, "Not running (pidfile not found)"));
	}

	@ParameterizedTest(name = "{0} {1}")
	@MethodSource("parameters")
	void forceStopWhenStopped(String os, String version) throws Exception {
		String output = doTest(os, version, "force-stop-when-stopped.sh");
		assertThat(output).contains("Status: 0");
		assertThat(output).has(coloredString(AnsiColor.YELLOW, "Not running (pidfile not found)"));
	}

	@ParameterizedTest(name = "{0} {1}")
	@MethodSource("parameters")
	void startWhenStarted(String os, String version) throws Exception {
		String output = doTest(os, version, "start-when-started.sh");
		assertThat(output).contains("Status: 0");
		assertThat(output).has(coloredString(AnsiColor.YELLOW, "Already running [" + extractPid(output) + "]"));
	}

	@ParameterizedTest(name = "{0} {1}")
	@MethodSource("parameters")
	void restartWhenStopped(String os, String version) throws Exception {
		String output = doTest(os, version, "restart-when-stopped.sh");
		assertThat(output).contains("Status: 0");
		assertThat(output).has(coloredString(AnsiColor.YELLOW, "Not running (pidfile not found)"));
		assertThat(output).has(coloredString(AnsiColor.GREEN, "Started [" + extractPid(output) + "]"));
	}

	@ParameterizedTest(name = "{0} {1}")
	@MethodSource("parameters")
	void restartWhenStarted(String os, String version) throws Exception {
		String output = doTest(os, version, "restart-when-started.sh");
		assertThat(output).contains("Status: 0");
		assertThat(output).has(coloredString(AnsiColor.GREEN, "Started [" + extract("PID1", output) + "]"));
		assertThat(output).has(coloredString(AnsiColor.GREEN, "Stopped [" + extract("PID1", output) + "]"));
		assertThat(output).has(coloredString(AnsiColor.GREEN, "Started [" + extract("PID2", output) + "]"));
	}

	@ParameterizedTest(name = "{0} {1}")
	@MethodSource("parameters")
	void startWhenStopped(String os, String version) throws Exception {
		String output = doTest(os, version, "start-when-stopped.sh");
		assertThat(output).contains("Status: 0");
		assertThat(output).has(coloredString(AnsiColor.GREEN, "Started [" + extractPid(output) + "]"));
	}

	@ParameterizedTest(name = "{0} {1}")
	@MethodSource("parameters")
	void basicLaunch(String os, String version) throws Exception {
		String output = doTest(os, version, "basic-launch.sh");
		assertThat(output).doesNotContain("PID_FOLDER");
	}

	@ParameterizedTest(name = "{0} {1}")
	@MethodSource("parameters")
	void launchWithMissingLogFolderGeneratesAWarning(String os, String version) throws Exception {
		String output = doTest(os, version, "launch-with-missing-log-folder.sh");
		assertThat(output).has(
				coloredString(AnsiColor.YELLOW, "LOG_FOLDER /does/not/exist does not exist. Falling back to /tmp"));
	}

	@ParameterizedTest(name = "{0} {1}")
	@MethodSource("parameters")
	void launchWithMissingPidFolderGeneratesAWarning(String os, String version) throws Exception {
		String output = doTest(os, version, "launch-with-missing-pid-folder.sh");
		assertThat(output).has(
				coloredString(AnsiColor.YELLOW, "PID_FOLDER /does/not/exist does not exist. Falling back to /tmp"));
	}

	@ParameterizedTest(name = "{0} {1}")
	@MethodSource("parameters")
	void launchWithSingleCommandLineArgument(String os, String version) throws Exception {
		doLaunch(os, version, "launch-with-single-command-line-argument.sh");
	}

	@ParameterizedTest(name = "{0} {1}")
	@MethodSource("parameters")
	void launchWithMultipleCommandLineArguments(String os, String version) throws Exception {
		doLaunch(os, version, "launch-with-multiple-command-line-arguments.sh");
	}

	@ParameterizedTest(name = "{0} {1}")
	@MethodSource("parameters")
	void launchWithSingleRunArg(String os, String version) throws Exception {
		doLaunch(os, version, "launch-with-single-run-arg.sh");
	}

	@ParameterizedTest(name = "{0} {1}")
	@MethodSource("parameters")
	void launchWithMultipleRunArgs(String os, String version) throws Exception {
		doLaunch(os, version, "launch-with-multiple-run-args.sh");
	}

	@ParameterizedTest(name = "{0} {1}")
	@MethodSource("parameters")
	void launchWithSingleJavaOpt(String os, String version) throws Exception {
		doLaunch(os, version, "launch-with-single-java-opt.sh");
	}

	@ParameterizedTest(name = "{0} {1}")
	@MethodSource("parameters")
	void launchWithDoubleLinkSingleJavaOpt(String os, String version) throws Exception {
		doLaunch(os, version, "launch-with-double-link-single-java-opt.sh");
	}

	@ParameterizedTest(name = "{0} {1}")
	@MethodSource("parameters")
	void launchWithMultipleJavaOpts(String os, String version) throws Exception {
		doLaunch(os, version, "launch-with-multiple-java-opts.sh");
	}

	@ParameterizedTest(name = "{0} {1}")
	@MethodSource("parameters")
	void launchWithUseOfStartStopDaemonDisabled(String os, String version) throws Exception {
		// CentOS doesn't have start-stop-daemon
		Assumptions.assumeFalse(os.equals("CentOS"));
		doLaunch(os, version, "launch-with-use-of-start-stop-daemon-disabled.sh");
	}

	@ParameterizedTest(name = "{0} {1}")
	@MethodSource("parameters")
	void launchWithRelativePidFolder(String os, String version) throws Exception {
		String output = doTest(os, version, "launch-with-relative-pid-folder.sh");
		assertThat(output).has(coloredString(AnsiColor.GREEN, "Started [" + extractPid(output) + "]"));
		assertThat(output).has(coloredString(AnsiColor.GREEN, "Running [" + extractPid(output) + "]"));
		assertThat(output).has(coloredString(AnsiColor.GREEN, "Stopped [" + extractPid(output) + "]"));
	}

	@ParameterizedTest(name = "{0} {1}")
	@MethodSource("parameters")
	void pidFolderOwnership(String os, String version) throws Exception {
		String output = doTest(os, version, "pid-folder-ownership.sh");
		assertThat(output).contains("phil root");
	}

	@ParameterizedTest(name = "{0} {1}")
	@MethodSource("parameters")
	void pidFileOwnership(String os, String version) throws Exception {
		String output = doTest(os, version, "pid-file-ownership.sh");
		assertThat(output).contains("phil root");
	}

	@ParameterizedTest(name = "{0} {1}")
	@MethodSource("parameters")
	void logFileOwnership(String os, String version) throws Exception {
		String output = doTest(os, version, "log-file-ownership.sh");
		assertThat(output).contains("phil root");
	}

	@ParameterizedTest(name = "{0} {1}")
	@MethodSource("parameters")
	void logFileOwnershipIsChangedWhenCreated(String os, String version) throws Exception {
		String output = doTest(os, version, "log-file-ownership-is-changed-when-created.sh");
		assertThat(output).contains("andy root");
	}

	@ParameterizedTest(name = "{0} {1}")
	@MethodSource("parameters")
	void logFileOwnershipIsUnchangedWhenExists(String os, String version) throws Exception {
		String output = doTest(os, version, "log-file-ownership-is-unchanged-when-exists.sh");
		assertThat(output).contains("root root");
	}

	@ParameterizedTest(name = "{0} {1}")
	@MethodSource("parameters")
	void launchWithRelativeLogFolder(String os, String version) throws Exception {
		String output = doTest(os, version, "launch-with-relative-log-folder.sh");
		assertThat(output).contains("Log written");
	}

	@ParameterizedTest(name = "{0} {1}")
	@MethodSource("parameters")
	void launchWithRunAsUser(String os, String version) throws Exception {
		String output = doTest(os, version, "launch-with-run-as-user.sh");
		assertThat(output).contains("wagner root");
	}

	@ParameterizedTest(name = "{0} {1}")
	@MethodSource("parameters")
	void whenRunAsUserDoesNotExistLaunchFailsWithInvalidArgument(String os, String version) throws Exception {
		String output = doTest(os, version, "launch-with-run-as-invalid-user.sh");
		assertThat(output).contains("Status: 2");
		assertThat(output).has(coloredString(AnsiColor.RED, "Cannot run as 'johndoe': no such user"));
	}

	@ParameterizedTest(name = "{0} {1}")
	@MethodSource("parameters")
	void whenJarOwnerAndRunAsUserAreBothSpecifiedRunAsUserTakesPrecedence(String os, String version) throws Exception {
		String output = doTest(os, version, "launch-with-run-as-user-preferred-to-jar-owner.sh");
		assertThat(output).contains("wagner root");
	}

	@ParameterizedTest(name = "{0} {1}")
	@MethodSource("parameters")
	void whenLaunchedUsingNonRootUserWithRunAsUserSpecifiedLaunchFailsWithInsufficientPrivilege(String os,
			String version) throws Exception {
		String output = doTest(os, version, "launch-with-run-as-user-root-required.sh");
		assertThat(output).contains("Status: 4");
		assertThat(output).has(coloredString(AnsiColor.RED, "Cannot run as 'wagner': current user is not root"));
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

}
