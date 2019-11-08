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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.cli.command.status.ExitStatus;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link CommandRunner}.
 *
 * @author Dave Syer
 * @author Andy Wilkinson
 */
class CommandRunnerIntegrationTests {

	@BeforeEach
	void clearDebug() {
		System.clearProperty("debug");
	}

	@Test
	void debugEnabledAndArgumentRemovedWhenNotAnApplicationArgument() {
		CommandRunner runner = new CommandRunner("spring");
		ArgHandlingCommand command = new ArgHandlingCommand();
		runner.addCommand(command);
		runner.runAndHandleErrors("args", "samples/app.groovy", "--debug");
		assertThat(command.args).containsExactly("samples/app.groovy");
		assertThat(System.getProperty("debug")).isEqualTo("true");
	}

	@Test
	void debugNotEnabledAndArgumentRetainedWhenAnApplicationArgument() {
		CommandRunner runner = new CommandRunner("spring");
		ArgHandlingCommand command = new ArgHandlingCommand();
		runner.addCommand(command);
		runner.runAndHandleErrors("args", "samples/app.groovy", "--", "--debug");
		assertThat(command.args).containsExactly("samples/app.groovy", "--", "--debug");
		assertThat(System.getProperty("debug")).isNull();
	}

	static class ArgHandlingCommand extends AbstractCommand {

		private String[] args;

		ArgHandlingCommand() {
			super("args", "");
		}

		@Override
		public ExitStatus run(String... args) throws Exception {
			this.args = args;
			return ExitStatus.OK;
		}

	}

}
