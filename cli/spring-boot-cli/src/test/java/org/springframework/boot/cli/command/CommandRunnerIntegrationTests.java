/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.cli.command;

import org.jspecify.annotations.Nullable;
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

		private String @Nullable [] args;

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
