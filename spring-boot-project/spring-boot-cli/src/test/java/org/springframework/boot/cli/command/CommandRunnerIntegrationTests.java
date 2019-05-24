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

package org.springframework.boot.cli.command;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.boot.cli.command.run.RunCommand;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link CommandRunner}.
 *
 * @author Dave Syer
 */
@ExtendWith(OutputCaptureExtension.class)
class CommandRunnerIntegrationTests {

	@Test
	void debugAddsAutoconfigReport(CapturedOutput capturedOutput) {
		CommandRunner runner = new CommandRunner("spring");
		runner.addCommand(new RunCommand());
		// -d counts as "debug" for the spring command, but not for the
		// LoggingApplicationListener
		runner.runAndHandleErrors("run", "samples/app.groovy", "-d");
		assertThat(capturedOutput).contains("Negative matches:");
	}

	@Test
	void debugSwitchedOffForAppArgs(CapturedOutput capturedOutput) {
		CommandRunner runner = new CommandRunner("spring");
		runner.addCommand(new RunCommand());
		runner.runAndHandleErrors("run", "samples/app.groovy", "--", "-d");
		assertThat(capturedOutput).doesNotContain("Negative matches:");
	}

}
