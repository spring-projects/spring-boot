/*
 * Copyright 2012-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.cli;

import java.io.IOException;

import org.junit.Test;

import org.springframework.boot.cli.infrastructure.CommandLineInvoker;
import org.springframework.boot.cli.infrastructure.CommandLineInvoker.Invocation;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;

/**
 * Integration Tests for the command line application.
 *
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
public class CommandLineIT {

	private final CommandLineInvoker cli = new CommandLineInvoker();

	@Test
	public void hintProducesListOfValidCommands()
			throws IOException, InterruptedException {
		Invocation cli = this.cli.invoke("hint");
		assertThat(cli.await(), equalTo(0));
		assertThat("Unexpected error: \n" + cli.getErrorOutput(),
				cli.getErrorOutput().length(), equalTo(0));
		assertThat(cli.getStandardOutputLines().size(), equalTo(10));
	}

	@Test
	public void invokingWithNoArgumentsDisplaysHelp()
			throws IOException, InterruptedException {
		Invocation cli = this.cli.invoke();
		assertThat(cli.await(), equalTo(1));
		assertThat(cli.getErrorOutput().length(), equalTo(0));
		assertThat(cli.getStandardOutput(), startsWith("usage:"));
	}

	@Test
	public void unrecognizedCommandsAreHandledGracefully()
			throws IOException, InterruptedException {
		Invocation cli = this.cli.invoke("not-a-real-command");
		assertThat(cli.await(), equalTo(1));
		assertThat(cli.getErrorOutput(),
				containsString("'not-a-real-command' is not a valid command"));
		assertThat(cli.getStandardOutput().length(), equalTo(0));
	}

	@Test
	public void version() throws IOException, InterruptedException {
		Invocation cli = this.cli.invoke("version");
		assertThat(cli.await(), equalTo(0));
		assertThat(cli.getErrorOutput().length(), equalTo(0));
		assertThat(cli.getStandardOutput(), startsWith("Spring CLI v"));
	}

	@Test
	public void help() throws IOException, InterruptedException {
		Invocation cli = this.cli.invoke("help");
		assertThat(cli.await(), equalTo(1));
		assertThat(cli.getErrorOutput().length(), equalTo(0));
		assertThat(cli.getStandardOutput(), startsWith("usage:"));
	}

}
