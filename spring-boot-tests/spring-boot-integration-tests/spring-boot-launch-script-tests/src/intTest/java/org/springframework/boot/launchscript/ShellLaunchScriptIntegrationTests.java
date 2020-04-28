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

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import org.springframework.boot.testsupport.testcontainers.DisabledIfDockerUnavailable;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests of Spring Boot's launch script with launching via shell.
 *
 * @author Alexey Vinogradov (vinogradov.a.i.93@gmail.com)
 */
@DisabledIfDockerUnavailable
class ShellLaunchScriptIntegrationTests extends BasicLaunchScriptIntegrationTests {

	@ParameterizedTest(name = "{0} {1}")
	@MethodSource("parameters")
	void basicLaunch(String os, String version) throws Exception {
		doLaunch(os, version, "jar/basic-launch.sh");
	}

	@ParameterizedTest(name = "{0} {1}")
	@MethodSource("parameters")
	void launchWithDebugEnv(String os, String version) throws Exception {
		final String output = doTest(os, version, "jar/launch-with-debug.sh");
		assertThat(output).contains("++ pwd");
	}

	@ParameterizedTest(name = "{0} {1}")
	@MethodSource("parameters")
	void launchWithDifferentJarFileEnv(String os, String version) throws Exception {
		final String output = doTest(os, version, "jar/launch-with-jarfile.sh");
		assertThat(output).contains("app-another.jar");
		assertThat(output).doesNotContain("app.jar");
	}

	@ParameterizedTest(name = "{0} {1}")
	@MethodSource("parameters")
	void launchWithDifferentAppName(String os, String version) throws Exception {
		final String output = doTest(os, version, "jar/launch-with-app-name.sh");
		assertThat(output).contains("All tests are passed.");
	}

	@ParameterizedTest(name = "{0} {1}")
	@MethodSource("parameters")
	void launchInInitdDir(String os, String version) throws Exception {
		final String output = doTest(os, version, "jar/launch-in-init.d-dir.sh");
		assertThat(output).contains("Usage: ./some_app {start|stop|force-stop|restart|force-reload|status|run}");
	}

}
