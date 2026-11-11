/*
 * Copyright 2012-present the original author or authors.
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
		assertThat(cli.getStandardOutputLines()).hasSize(5);
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
