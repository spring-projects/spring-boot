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

import groovy.lang.GroovyObjectSupport;
import groovy.lang.Script;

import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.boot.cli.GrapesCleaner;
import org.springframework.boot.cli.command.OptionHandler;
import org.springframework.boot.cli.command.ScriptCommand;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

/**
 * Tests for {@link ScriptCommand}.
 * 
 * @author Dave Syer
 */
public class ScriptCommandTests {

	public static boolean executed = false;

	@BeforeClass
	public static void cleanGrapes() throws Exception {
		GrapesCleaner.cleanIfNecessary();
	}

	@Test(expected = IllegalStateException.class)
	public void testMissing() throws Exception {
		ScriptCommand command = new ScriptCommand("missing");
		command.run("World");
	}

	@Test
	public void testScript() throws Exception {
		ScriptCommand command = new ScriptCommand("script");
		command.run("World");
		assertEquals("World",
				((String[]) ((Script) command.getMain()).getProperty("args"))[0]);
	}

	@Test
	public void testLocateFile() throws Exception {
		ScriptCommand command = new ScriptCommand(
				"src/test/resources/commands/script.groovy");
		command.setPaths(new String[] { "." });
		command.run("World");
		assertEquals("World",
				((String[]) ((Script) command.getMain()).getProperty("args"))[0]);
	}

	@Test
	public void testRunnable() throws Exception {
		ScriptCommand command = new ScriptCommand("runnable");
		command.run("World");
		assertTrue(executed);
	}

	@Test
	public void testClosure() throws Exception {
		ScriptCommand command = new ScriptCommand("closure");
		command.run("World");
		assertTrue(executed);
	}

	@Test
	public void testCommand() throws Exception {
		ScriptCommand command = new ScriptCommand("command");
		assertEquals("My script command", command.getUsageHelp());
		command.run("World");
		assertTrue(executed);
	}

	@Test
	public void testDuplicateClassName() throws Exception {
		ScriptCommand command1 = new ScriptCommand("handler");
		ScriptCommand command2 = new ScriptCommand("command");
		assertNotSame(command1.getMain().getClass(), command2.getMain().getClass());
		assertEquals(command1.getMain().getClass().getName(), command2.getMain()
				.getClass().getName());
	}

	@Test
	public void testOptions() throws Exception {
		ScriptCommand command = new ScriptCommand("handler");
		String out = ((OptionHandler) command.getMain()).getHelp();
		assertTrue("Wrong output: " + out, out.contains("--foo"));
		command.run("World", "--foo");
		assertTrue(executed);
	}

	@Test
	public void testMixin() throws Exception {
		ScriptCommand command = new ScriptCommand("mixin");
		GroovyObjectSupport object = (GroovyObjectSupport) command.getMain();
		String out = (String) object.getProperty("help");
		assertTrue("Wrong output: " + out, out.contains("--foo"));
		command.run("World", "--foo");
		assertTrue(executed);
	}

	@Test
	public void testMixinWithBlock() throws Exception {
		ScriptCommand command = new ScriptCommand("test");
		GroovyObjectSupport object = (GroovyObjectSupport) command.getMain();
		String out = (String) object.getProperty("help");
		System.err.println(out);
		assertTrue("Wrong output: " + out, out.contains("--foo"));
		command.run("World", "--foo", "--bar=2");
		assertTrue(executed);
	}

}
