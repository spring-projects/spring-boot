/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.cli.command.test;

import joptsimple.OptionSet;

import org.springframework.boot.cli.command.Command;
import org.springframework.boot.cli.command.OptionParsingCommand;
import org.springframework.boot.cli.command.options.CompilerOptionHandler;
import org.springframework.boot.cli.command.options.OptionSetGroovyCompilerConfiguration;
import org.springframework.boot.cli.command.options.SourceOptions;
import org.springframework.boot.cli.command.status.ExitStatus;

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

	private static class TestOptionHandler extends CompilerOptionHandler {

		private TestRunner runner;

		@Override
		protected ExitStatus run(OptionSet options) throws Exception {
			SourceOptions sourceOptions = new SourceOptions(options);
			TestRunnerConfiguration configuration = new TestRunnerConfigurationAdapter(
					options, this);
			this.runner = new TestRunner(configuration, sourceOptions.getSourcesArray());
			this.runner.compileAndRunTests();
			return ExitStatus.OK.hangup();
		}

		/**
		 * Simple adapter class to present the {@link OptionSet} as a
		 * {@link TestRunnerConfiguration}.
		 */
		private class TestRunnerConfigurationAdapter extends
				OptionSetGroovyCompilerConfiguration implements TestRunnerConfiguration {

			TestRunnerConfigurationAdapter(OptionSet options,
					CompilerOptionHandler optionHandler) {
				super(options, optionHandler);
			}

		}

	}

}
