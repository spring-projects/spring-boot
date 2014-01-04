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

package cli.command;

import groovy.lang.Closure;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.boot.OutputCapture;
import org.springframework.boot.cli.Command;
import org.springframework.boot.cli.command.InitCommand.Commands;
import org.springframework.boot.cli.command.OptionHandler;
import org.springframework.boot.cli.command.ScriptCompilationCustomizer;
import org.springframework.boot.cli.compiler.GroovyCompiler;
import org.springframework.boot.cli.compiler.GroovyCompilerConfiguration;
import org.springframework.boot.cli.compiler.GroovyCompilerScope;
import org.springframework.boot.cli.compiler.grape.RepositoryConfiguration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Dave Syer
 */
public class ScriptCompilationCustomizerTests {

	private TestGroovyCompilerConfiguration configuration = new TestGroovyCompilerConfiguration();
	private GroovyCompiler compiler = new GroovyCompiler(this.configuration);

	@Rule
	public OutputCapture output = new OutputCapture();

	@Before
	public void init() {
		this.compiler.addCompilationCustomizers(new ScriptCompilationCustomizer());
	}

	@Test
	public void simpleCompile() throws Exception {
		Class<?>[] types = this.compiler.compile(new File(
				"src/test/resources/scripts/command.groovy"));
		Class<?> main = types[0];
		assertEquals("org.test.command.TestCommand", main.getName());
		assertTrue(Command.class.isAssignableFrom(main));
	}

	@Test
	public void addsOptionHandler() throws Exception {
		Class<?>[] types = this.compiler.compile(new File(
				"src/test/resources/scripts/handler.groovy"));
		Class<?> main = types[0];
		assertTrue(OptionHandler.class.isAssignableFrom(main));
	}

	@Test
	public void addsCommands() throws Exception {
		Class<?>[] types = this.compiler.compile(new File(
				"src/test/resources/scripts/options.groovy"));
		Class<?> main = types[0];
		assertTrue(Commands.class.isAssignableFrom(main));
	}

	@Test
	public void commandsExecutable() throws Exception {
		Class<?>[] types = this.compiler.compile(new File(
				"src/test/resources/scripts/options.groovy"));
		Class<?> main = types[0];
		Map<String, Closure<?>> commands = ((Commands) main.newInstance()).getCommands();
		assertEquals(1, commands.size());
		assertEquals("foo", commands.keySet().iterator().next());
		Closure<?> closure = commands.values().iterator().next();
		closure.call(); // what about args?
		assertTrue(this.output.toString().contains("Hello Command"));
	}

	private static class TestGroovyCompilerConfiguration implements
			GroovyCompilerConfiguration {

		@Override
		public GroovyCompilerScope getScope() {
			return GroovyCompilerScope.EXTENSION;
		}

		@Override
		public boolean isGuessImports() {
			return true;
		}

		@Override
		public boolean isGuessDependencies() {
			return false;
		}

		@Override
		public boolean isAutoconfigure() {
			return true;
		}

		@Override
		public String[] getClasspath() {
			return new String[0];
		}

		@Override
		public List<RepositoryConfiguration> getRepositoryConfiguration() {
			return Collections.emptyList();
		}

	}

}
