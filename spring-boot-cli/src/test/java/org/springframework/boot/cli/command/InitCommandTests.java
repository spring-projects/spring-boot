/*
 * Copyright 2012-2013 the original author or authors.
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

package org.springframework.boot.cli.command;

import groovy.lang.GroovyClassLoader;

import java.util.Random;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.boot.OutputCapture;
import org.springframework.boot.cli.Command;
import org.springframework.boot.cli.SpringCli;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author Dave Syer
 */
public class InitCommandTests {

	@Rule
	public OutputCapture output = new OutputCapture();

	private SpringCli cli = mock(SpringCli.class);
	private InitCommand command = new InitCommand(this.cli);
	private int defaultCount = new DefaultCommandFactory().getCommands(this.cli).size();
	private ClassLoader classLoader;

	@Before
	public void init() {
		this.classLoader = Thread.currentThread().getContextClassLoader();
	}

	@After
	public void close() {
		Thread.currentThread().setContextClassLoader(this.classLoader);
	}

	@Test
	public void explicitClasspath() throws Exception {
		Thread.currentThread().setContextClassLoader(new GroovyClassLoader());
		this.command.run("--cp=src/test/plugins/custom/custom/0.0.1/custom-0.0.1.jar");
		verify(this.cli, times(this.defaultCount + 1)).register(any(Command.class));
	}

	@Test
	public void initScript() throws Exception {
		this.command.run("src/test/resources/grab.groovy");
		verify(this.cli, times(this.defaultCount + 1)).register(any(Command.class));
		assertTrue(this.output.toString().contains("Hello Grab"));
	}

	@Test
	public void initCommand() throws Exception {
		this.command.run("src/test/resources/commands/command.groovy");
		verify(this.cli, times(this.defaultCount + 1)).register(any(Command.class));
	}

	@Test
	public void initHandler() throws Exception {
		this.command.run("src/test/resources/commands/handler.groovy");
		verify(this.cli, times(this.defaultCount + 1)).register(any(Command.class));
	}

	@Test
	public void initClosure() throws Exception {
		this.command.run("src/test/resources/commands/closure.groovy");
		verify(this.cli, times(this.defaultCount + 1)).register(any(Command.class));
	}

	@Test
	public void initOptions() throws Exception {
		this.command.run("src/test/resources/commands/options.groovy");
		verify(this.cli, times(this.defaultCount + 1)).register(any(Command.class));
	}

	@Test
	public void runOptions() throws Exception {
		SpringCli cli = new SpringCli();
		InitCommand command = cli.getInitCommand();
		command.run("src/test/resources/commands/options.groovy");
		cli.find("foo").run("--foo=bar", "--bar=123");
		assertTrue(this.output.toString().contains("Hello Foo: bar=123"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void initNonExistentScript() throws Exception {
		this.command.run("nonexistent.groovy");
	}

	@Test
	public void initDefault() throws Exception {
		this.command.run();
		assertFalse(this.output.toString().contains("Hello Init"));
	}

	@Test
	public void initWithCommandline() throws Exception {
		this.command.run("--init=init.groovy");
		assertTrue(this.output.toString().contains("Hello Init"));
	}

	public static void main(String[] args) throws Exception {
		Random random = new Random();
		InitCommandTests test = new InitCommandTests();
		test.init();
		SpringCli cli = new SpringCli();
		while (true) {
			InitCommand command = new InitCommand(cli);
			command.run("--init=file:" + random.nextInt() + ".groovy");
			test.close();
		}
	}

}
