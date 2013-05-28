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
package org.springframework.bootstrap.cli.command;

import groovy.lang.Script;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Dave Syer
 * 
 */
public class ScriptCommandTests {

	public static boolean executed = false;

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
	public void testCommand() throws Exception {
		ScriptCommand command = new ScriptCommand("command");
		assertEquals("My script command", command.getUsageHelp());
		command.run("World");
		assertTrue(executed);
	}

	@Test
	public void testOptions() throws Exception {
		ScriptCommand command = new ScriptCommand("test");
		command.run("World", "--foo");
		String out = ((OptionHandler) command.getMain()).getHelp();
		assertTrue("Wrong output: " + out, out.contains("--foo"));
		assertTrue(executed);
	}

}
