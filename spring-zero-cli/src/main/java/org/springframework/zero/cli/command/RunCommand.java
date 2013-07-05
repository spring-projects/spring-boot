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

package org.springframework.zero.cli.command;

import java.awt.Desktop;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import joptsimple.OptionSet;
import joptsimple.OptionSpec;

import org.springframework.zero.cli.Command;
import org.springframework.zero.cli.runner.SpringZeroRunner;
import org.springframework.zero.cli.runner.SpringZeroRunnerConfiguration;

import static java.util.Arrays.asList;

/**
 * {@link Command} to 'run' a groovy script or scripts.
 * 
 * @author Phillip Webb
 * @author Dave Syer
 * @see SpringZeroRunner
 */
public class RunCommand extends OptionParsingCommand {

	public RunCommand() {
		super("run", "Run a spring groovy script", new RunOptionHandler());
	}

	@Override
	public String getUsageHelp() {
		return "[options] <files> [--] [args]";
	}

	public void stop() {
		if (this.getHandler() != null) {
			((RunOptionHandler) this.getHandler()).runner.stop();
		}
	}

	private static class RunOptionHandler extends OptionHandler {

		private OptionSpec<Void> watchOption;

		private OptionSpec<Void> editOption;

		private OptionSpec<Void> noGuessImportsOption;

		private OptionSpec<Void> noGuessDependenciesOption;

		private OptionSpec<Void> verboseOption;

		private OptionSpec<Void> quietOption;

		private OptionSpec<Void> localOption;

		private SpringZeroRunner runner;

		@Override
		protected void options() {
			this.watchOption = option("watch", "Watch the specified file for changes");
			this.localOption = option("local",
					"Accumulate the dependencies in a local folder (./grapes)");
			this.editOption = option(asList("edit", "e"),
					"Open the file with the default system editor");
			this.noGuessImportsOption = option("no-guess-imports",
					"Do not attempt to guess imports");
			this.noGuessDependenciesOption = option("no-guess-dependencies",
					"Do not attempt to guess dependencies");
			this.verboseOption = option(asList("verbose", "v"), "Verbose logging");
			this.quietOption = option(asList("quiet", "q"), "Quiet logging");
		}

		@Override
		protected void run(OptionSet options) throws Exception {
			List<String> nonOptionArguments = options.nonOptionArguments();
			File[] files = getFileArguments(nonOptionArguments);
			List<String> args = nonOptionArguments.subList(files.length,
					nonOptionArguments.size());

			if (options.has(this.editOption)) {
				Desktop.getDesktop().edit(files[0]);
			}

			SpringZeroRunnerConfiguration configuration = new SpringZeroRunnerConfigurationAdapter(
					options);
			if (configuration.isLocal() && System.getProperty("grape.root") == null) {
				System.setProperty("grape.root", ".");
			}
			this.runner = new SpringZeroRunner(configuration, files,
					args.toArray(new String[args.size()]));
			this.runner.compileAndRun();
		}

		private File[] getFileArguments(List<String> nonOptionArguments) {
			List<File> files = new ArrayList<File>();
			for (String filename : nonOptionArguments) {
				if ("--".equals(filename)) {
					break;
				}
				// TODO: add support for strict Java compilation
				// TODO: add support for recursive search in directory
				if (filename.endsWith(".groovy") || filename.endsWith(".java")) {
					File file = new File(filename);
					if (file.isFile() && file.canRead()) {
						files.add(file);
					}
				}
			}
			if (files.size() == 0) {
				throw new RuntimeException("Please specify a file to run");
			}
			return files.toArray(new File[files.size()]);
		}

		/**
		 * Simple adapter class to present the {@link OptionSet} as a
		 * {@link SpringZeroRunnerConfiguration}.
		 */
		private class SpringZeroRunnerConfigurationAdapter implements
				SpringZeroRunnerConfiguration {

			private OptionSet options;

			public SpringZeroRunnerConfigurationAdapter(OptionSet options) {
				this.options = options;
			}

			@Override
			public boolean isWatchForFileChanges() {
				return this.options.has(RunOptionHandler.this.watchOption);
			}

			@Override
			public boolean isGuessImports() {
				return !this.options.has(RunOptionHandler.this.noGuessImportsOption);
			}

			@Override
			public boolean isGuessDependencies() {
				return !this.options.has(RunOptionHandler.this.noGuessDependenciesOption);
			}

			@Override
			public boolean isLocal() {
				return this.options.has(RunOptionHandler.this.localOption);
			}

			@Override
			public Level getLogLevel() {
				if (this.options.has(RunOptionHandler.this.verboseOption)) {
					return Level.FINEST;
				}
				if (this.options.has(RunOptionHandler.this.quietOption)) {
					return Level.OFF;
				}
				return Level.INFO;
			}

		}
	}

}
