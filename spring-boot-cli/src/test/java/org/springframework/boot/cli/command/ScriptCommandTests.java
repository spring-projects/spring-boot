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

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.boot.OutputCapture;
import org.springframework.boot.cli.SpringCli;

import static org.junit.Assert.assertTrue;

/**
 * @author Dave Syer
 */
public class ScriptCommandTests {

	@Rule
	public OutputCapture output = new OutputCapture();

	public static boolean executed = false;

	private SpringCli cli;
	private InitCommand init;

	private ClassLoader classLoader;

	@Before
	public void init() {
		this.classLoader = Thread.currentThread().getContextClassLoader();
		this.cli = new SpringCli();
		this.init = new InitCommand(this.cli);
		executed = false;
	}

	@After
	public void close() {
		Thread.currentThread().setContextClassLoader(this.classLoader);
	}

	@Test
	public void command() throws Exception {
		this.init.run("src/test/resources/commands/command.groovy");
		this.cli.find("foo").run("Foo");
		assertTrue(this.output.toString().contains("Hello Foo"));
	}

	@Test
	public void handler() throws Exception {
		this.init.run("src/test/resources/commands/handler.groovy");
		this.cli.find("foo").run("Foo", "--foo=bar");
		assertTrue(this.output.toString().contains("Hello [Foo]"));
	}

	@Test
	public void options() throws Exception {
		this.init.run("src/test/resources/commands/options.groovy");
		this.cli.find("foo").run("Foo", "--foo=bar");
		assertTrue(this.output.toString().contains("Hello Foo"));
	}

}
