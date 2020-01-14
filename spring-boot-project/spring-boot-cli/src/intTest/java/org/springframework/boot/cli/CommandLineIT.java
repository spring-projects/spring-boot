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

package org.springframework.boot.cli;

import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.boot.cli.infrastructure.CommandLineInvoker;
import org.springframework.boot.cli.infrastructure.CommandLineInvoker.Invocation;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration Tests for the command line application.
 *
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class CommandLineIT {

	private CommandLineInvoker cli;

	@BeforeEach
	void setup(@TempDir File tempDir) {
		this.cli = new CommandLineInvoker(tempDir);
	}

	@Test
	void hintProducesListOfValidCommands() throws IOException, InterruptedException {
		Invocation cli = this.cli.invoke("hint");
		assertThat(cli.await()).isEqualTo(0);
		assertThat(cli.getErrorOutput()).isEmpty();
		assertThat(cli.getStandardOutputLines()).hasSize(11);
	}

	@Test
	void invokingWithNoArgumentsDisplaysHelp() throws IOException, InterruptedException {
		Invocation cli = this.cli.invoke();
		assertThat(cli.await()).isEqualTo(1);
		assertThat(cli.getErrorOutput()).isEmpty();
		assertThat(cli.getStandardOutput()).startsWith("usage:");
	}

	@Test
	void unrecognizedCommandsAreHandledGracefully() throws IOException, InterruptedException {
		Invocation cli = this.cli.invoke("not-a-real-command");
		assertThat(cli.await()).isEqualTo(1);
		assertThat(cli.getErrorOutput()).contains("'not-a-real-command' is not a valid command");
		assertThat(cli.getStandardOutput()).isEmpty();
	}

	@Test
	void version() throws IOException, InterruptedException {
		Invocation cli = this.cli.invoke("version");
		assertThat(cli.await()).isEqualTo(0);
		assertThat(cli.getErrorOutput()).isEmpty();
		assertThat(cli.getStandardOutput()).startsWith("Spring CLI v");
	}

	@Test
	void help() throws IOException, InterruptedException {
		Invocation cli = this.cli.invoke("help");
		assertThat(cli.await()).isEqualTo(1);
		assertThat(cli.getErrorOutput()).isEmpty();
		assertThat(cli.getStandardOutput()).startsWith("usage:");
	}

}
