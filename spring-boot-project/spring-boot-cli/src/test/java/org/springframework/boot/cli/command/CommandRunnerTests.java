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

package org.springframework.boot.cli.command;

import java.util.EnumSet;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.boot.cli.command.core.HelpCommand;
import org.springframework.boot.cli.command.core.HintCommand;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.lenient;

/**
 * Tests for {@link CommandRunner}.
 *
 * @author Phillip Webb
 * @author Dave Syer
 */
@ExtendWith(MockitoExtension.class)
class CommandRunnerTests {

	private CommandRunner commandRunner;

	@Mock
	private Command regularCommand;

	@Mock
	private Command anotherCommand;

	private final Set<Call> calls = EnumSet.noneOf(Call.class);

	private ClassLoader loader;

	@AfterEach
	void close() {
		Thread.currentThread().setContextClassLoader(this.loader);
		System.clearProperty("debug");
	}

	@BeforeEach
	void setup() {
		this.loader = Thread.currentThread().getContextClassLoader();
		this.commandRunner = new CommandRunner("spring") {

			@Override
			protected void showUsage() {
				CommandRunnerTests.this.calls.add(Call.SHOW_USAGE);
				super.showUsage();
			}

			@Override
			protected boolean errorMessage(String message) {
				CommandRunnerTests.this.calls.add(Call.ERROR_MESSAGE);
				return super.errorMessage(message);
			}

			@Override
			protected void printStackTrace(Exception ex) {
				CommandRunnerTests.this.calls.add(Call.PRINT_STACK_TRACE);
				super.printStackTrace(ex);
			}
		};
		lenient().doReturn("another").when(this.anotherCommand).getName();
		lenient().doReturn("command").when(this.regularCommand).getName();
		lenient().doReturn("A regular command").when(this.regularCommand).getDescription();
		this.commandRunner.addCommand(this.regularCommand);
		this.commandRunner.addCommand(new HelpCommand(this.commandRunner));
		this.commandRunner.addCommand(new HintCommand(this.commandRunner));
	}

	@Test
	void runWithoutArguments() {
		assertThatExceptionOfType(NoArgumentsException.class).isThrownBy(this.commandRunner::run);
	}

	@Test
	void runCommand() throws Exception {
		this.commandRunner.run("command", "--arg1", "arg2");
		then(this.regularCommand).should().run("--arg1", "arg2");
	}

	@Test
	void missingCommand() {
		assertThatExceptionOfType(NoSuchCommandException.class).isThrownBy(() -> this.commandRunner.run("missing"));
	}

	@Test
	void appArguments() throws Exception {
		this.commandRunner.runAndHandleErrors("command", "--", "--debug", "bar");
		then(this.regularCommand).should().run("--", "--debug", "bar");
		// When handled by the command itself it shouldn't cause the system property to be
		// set
		assertThat(System.getProperty("debug")).isNull();
	}

	@Test
	void handlesSuccess() {
		int status = this.commandRunner.runAndHandleErrors("command");
		assertThat(status).isEqualTo(0);
		assertThat(this.calls).isEmpty();
	}

	@Test
	void handlesNoSuchCommand() {
		int status = this.commandRunner.runAndHandleErrors("missing");
		assertThat(status).isEqualTo(1);
		assertThat(this.calls).containsOnly(Call.ERROR_MESSAGE);
	}

	@Test
	void handlesRegularExceptionWithMessage() throws Exception {
		willThrow(new RuntimeException("With Message")).given(this.regularCommand).run();
		int status = this.commandRunner.runAndHandleErrors("command");
		assertThat(status).isEqualTo(1);
		assertThat(this.calls).containsOnly(Call.ERROR_MESSAGE);
	}

	@Test
	void handlesRegularExceptionWithoutMessage() throws Exception {
		willThrow(new RuntimeException()).given(this.regularCommand).run();
		int status = this.commandRunner.runAndHandleErrors("command");
		assertThat(status).isEqualTo(1);
		assertThat(this.calls).containsOnly(Call.ERROR_MESSAGE, Call.PRINT_STACK_TRACE);
	}

	@Test
	void handlesExceptionWithDashDashDebug() throws Exception {
		willThrow(new RuntimeException()).given(this.regularCommand).run();
		int status = this.commandRunner.runAndHandleErrors("command", "--debug");
		assertThat(System.getProperty("debug")).isEqualTo("true");
		assertThat(status).isEqualTo(1);
		assertThat(this.calls).containsOnly(Call.ERROR_MESSAGE, Call.PRINT_STACK_TRACE);
	}

	@Test
	void exceptionMessages() {
		assertThat(new NoSuchCommandException("name").getMessage())
				.isEqualTo("'name' is not a valid command. See 'help'.");
	}

	@Test
	void help() throws Exception {
		this.commandRunner.run("help", "command");
		then(this.regularCommand).should().getHelp();
	}

	@Test
	void helpNoCommand() {
		assertThatExceptionOfType(NoHelpCommandArgumentsException.class)
				.isThrownBy(() -> this.commandRunner.run("help"));
	}

	@Test
	void helpUnknownCommand() {
		assertThatExceptionOfType(NoSuchCommandException.class)
				.isThrownBy(() -> this.commandRunner.run("help", "missing"));
	}

	private enum Call {

		SHOW_USAGE, ERROR_MESSAGE, PRINT_STACK_TRACE

	}

}
