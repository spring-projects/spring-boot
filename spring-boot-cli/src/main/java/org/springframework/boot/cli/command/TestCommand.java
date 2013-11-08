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

import joptsimple.OptionSet;
import joptsimple.OptionSpec;

import org.springframework.boot.cli.Command;
import org.springframework.boot.cli.compiler.GroovyCompilerScope;
import org.springframework.boot.cli.testrunner.TestRunner;
import org.springframework.boot.cli.testrunner.TestRunnerConfiguration;

import static java.util.Arrays.asList;

/**
 * {@link Command} to run a groovy test script or scripts.
 * 
 * @author Greg Turnquist
 * @author Phillip Webb
 */
public class TestCommand extends OptionParsingCommand {

	public TestCommand() {
		super("test", "Run a spring groovy script test", new TestOptionHandler());
	}

	@Override
	public String getUsageHelp() {
		return "[options] <files> [--] [args]";
	}

	private static class TestOptionHandler extends OptionHandler {

		private OptionSpec<Void> noGuessImportsOption;

		private OptionSpec<Void> noGuessDependenciesOption;

		private OptionSpec<String> classpathOption;

		private TestRunner runner;

		@Override
		protected void options() {
			this.noGuessImportsOption = option("no-guess-imports",
					"Do not attempt to guess imports");
			this.noGuessDependenciesOption = option("no-guess-dependencies",
					"Do not attempt to guess dependencies");
			this.classpathOption = option(asList("classpath", "cp"),
					"Additional classpath entries").withRequiredArg();
		}

		@Override
		protected void run(OptionSet options) throws Exception {
			FileOptions fileOptions = new FileOptions(options);
			TestRunnerConfiguration configuration = new TestRunnerConfigurationAdapter(
					options);
			this.runner = new TestRunner(configuration, fileOptions.getFilesArray(),
					fileOptions.getArgsArray());
			this.runner.compileAndRunTests();
		}

		/**
		 * Simple adapter class to present the {@link OptionSet} as a
		 * {@link TestRunnerConfiguration}.
		 */
		private class TestRunnerConfigurationAdapter implements TestRunnerConfiguration {

			private OptionSet options;

			public TestRunnerConfigurationAdapter(OptionSet options) {
				this.options = options;
			}

			@Override
			public GroovyCompilerScope getScope() {
				return GroovyCompilerScope.DEFAULT;
			}

			@Override
			public boolean isGuessImports() {
				return !this.options.has(TestOptionHandler.this.noGuessImportsOption);
			}

			@Override
			public boolean isGuessDependencies() {
				return !this.options
						.has(TestOptionHandler.this.noGuessDependenciesOption);
			}

			@Override
			public String[] getClasspath() {
				if (this.options.has(TestOptionHandler.this.classpathOption)) {
					return this.options.valueOf(TestOptionHandler.this.classpathOption)
							.split(":");
				}
				return NO_CLASSPATH;
			}

		}
	}
}
