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

import java.util.Random;
import java.util.ServiceLoader;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.boot.OutputCapture;
import org.springframework.boot.cli.Command;
import org.springframework.boot.cli.CommandFactory;
import org.springframework.boot.cli.SpringCli;

/**
 * @author Dave Syer
 */
public class InitCommandPerformanceTests {

	@Rule
	public OutputCapture output = new OutputCapture();
	private ClassLoader classLoader;
	private Random random = new Random();

	@Before
	public void init() {
		this.classLoader = Thread.currentThread().getContextClassLoader();
	}

	@After
	public void close() {
		Thread.currentThread().setContextClassLoader(this.classLoader);
	}

	@Test
	public void initDefault() throws Exception {
		for (int i = 0; i < 100; i++) {
			SpringCli cli = new SpringCli();
			InitCommand command = new InitCommand(cli);
			command.run();
			close();
		}
	}

	@Test
	// Fast...
	public void initNonExistent() throws Exception {
		for (int i = 0; i < 100; i++) {
			SpringCli cli = new SpringCli();
			InitCommand command = new InitCommand(cli);
			command.run("--init=" + this.random.nextInt() + ".groovy");
			close();
		}
	}

	@Test
	// Fast...
	public void fakeCommand() throws Exception {
		final SpringCli cli = new SpringCli();
		for (int i = 0; i < 100; i++) {
			Command command = new AbstractCommand("fake", "") {
				@Override
				public void run(String... args) throws Exception {
					for (CommandFactory factory : ServiceLoader.load(
							CommandFactory.class, Thread.currentThread()
									.getContextClassLoader())) {
						for (Command command : factory.getCommands(cli)) {
							cli.register(command);
						}
					}
				}
			};
			command.run("--init=" + this.random.nextInt() + ".groovy");
			close();
		}
	}

	@Test
	// Fast...
	public void initNonExistentWithPrefix() throws Exception {
		for (int i = 0; i < 100; i++) {
			SpringCli cli = new SpringCli();
			InitCommand command = new InitCommand(cli);
			command.run("--init=file:" + this.random.nextInt() + ".groovy");
			close();
		}
	}

	@Test
	// There is an init.groovy on the test classpath so this succeeds
	// Slow...
	public void initFromClasspath() throws Exception {
		for (int i = 0; i < 5; i++) {
			SpringCli cli = new SpringCli();
			InitCommand command = new InitCommand(cli);
			command.run("--init=init.groovy");
			close();
		}
	}

	public static void main(String[] args) {
		SpringCli.main("hint");
	}

}
