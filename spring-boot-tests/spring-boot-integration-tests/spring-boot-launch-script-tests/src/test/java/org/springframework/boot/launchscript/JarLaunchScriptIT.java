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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests of Spring Boot's launch script when executing the jar directly.
 *
 * @author Alexey Vinogradov
 * @author Andy Wilkinson
 */
class JarLaunchScriptIT extends AbstractLaunchScriptIT {

	JarLaunchScriptIT() {
		super("jar/");
	}

	@ParameterizedTest(name = "{0} {1}")
	@MethodSource("parameters")
	void basicLaunch(String os, String version) throws Exception {
		doLaunch(os, version, "basic-launch.sh");
	}

	@ParameterizedTest(name = "{0} {1}")
	@MethodSource("parameters")
	void launchWithDebugEnv(String os, String version) throws Exception {
		final String output = doTest(os, version, "launch-with-debug.sh");
		assertThat(output).contains("++ pwd");
	}

	@ParameterizedTest(name = "{0} {1}")
	@MethodSource("parameters")
	void launchWithDifferentJarFileEnv(String os, String version) throws Exception {
		final String output = doTest(os, version, "launch-with-jarfile.sh");
		assertThat(output).contains("app-another.jar");
		assertThat(output).doesNotContain("spring-boot-launch-script-tests.jar");
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
	void launchWithMultipleJavaOpts(String os, String version) throws Exception {
		doLaunch(os, version, "launch-with-multiple-java-opts.sh");
	}

}
