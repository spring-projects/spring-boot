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

package org.springframework.bootstrap.cli;

import java.awt.Desktop;
import java.io.File;
import java.util.List;
import java.util.logging.Level;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

import org.springframework.bootstrap.cli.runner.BootstrapRunner;
import org.springframework.bootstrap.cli.runner.BootstrapRunnerConfiguration;

import static java.util.Arrays.asList;

/**
 * {@link Command} to 'run' a spring groovy script.
 * 
 * @author Phillip Webb
 * @see BootstrapRunner
 */
public class RunCommand extends OptionParsingCommand {

	private OptionSpec<Void> noWatchOption; // FIXME

	private OptionSpec<Void> editOption;

	private OptionSpec<Void> noGuessImportsOption;

	private OptionSpec<Void> noGuessDependenciesOption;

	private OptionSpec<Void> verboseOption;

	private OptionSpec<Void> quiteOption;

	public RunCommand() {
		super("run", "Run a spring groovy script");
	}

	@Override
	public String getUsageHelp() {
		return "[options] <file>";
	}

	@Override
	protected OptionParser createOptionParser() {
		OptionParser parser = new OptionParser();
		this.noWatchOption = parser.accepts("no-watch",
				"Do not watch the specified file for changes");
		this.editOption = parser.acceptsAll(asList("edit", "e"),
				"Open the file with the default system editor");
		this.noGuessImportsOption = parser.accepts("no-guess-imports",
				"Do not attempt to guess imports");
		this.noGuessDependenciesOption = parser.accepts("no-guess-dependencies",
				"Do not attempt to guess dependencies");
		this.verboseOption = parser.acceptsAll(asList("verbose", "v"), "Verbose logging");
		this.quiteOption = parser.acceptsAll(asList("quiet", "q"), "Quiet logging");
		return parser;
	}

	@Override
	protected void run(OptionSet options) throws Exception {
		List<String> nonOptionArguments = options.nonOptionArguments();
		File file = getFileArgument(nonOptionArguments);
		List<String> args = nonOptionArguments.subList(1, nonOptionArguments.size());

		if (options.has(this.editOption)) {
			Desktop.getDesktop().edit(file);
		}

		BootstrapRunnerConfiguration configuration = new BootstrapRunnerConfigurationAdapter(
				options);
		new BootstrapRunner(configuration, file, args.toArray(new String[args.size()]))
				.compileAndRun();
	}

	private File getFileArgument(List<String> nonOptionArguments) {
		if (nonOptionArguments.size() == 0) {
			throw new RuntimeException("Please specify a file to run");
		}
		String filename = nonOptionArguments.get(0);
		File file = new File(filename);
		if (!file.isFile() || !file.canRead()) {
			throw new RuntimeException("Unable to read '" + filename + "'");
		}
		return file;
	}

	/**
	 * Simple adapter class to present the {@link OptionSet} as a
	 * {@link BootstrapRunnerConfiguration}.
	 */
	private class BootstrapRunnerConfigurationAdapter implements
			BootstrapRunnerConfiguration {

		private OptionSet options;

		public BootstrapRunnerConfigurationAdapter(OptionSet options) {
			this.options = options;
		}

		@Override
		public boolean isWatchForFileChanges() {
			return !this.options.has(RunCommand.this.noWatchOption);
		}

		@Override
		public boolean isGuessImports() {
			return !this.options.has(RunCommand.this.noGuessImportsOption);
		}

		@Override
		public boolean isGuessDependencies() {
			return !this.options.has(RunCommand.this.noGuessDependenciesOption);
		}

		@Override
		public Level getLogLevel() {
			if (this.options.has(RunCommand.this.verboseOption)) {
				return Level.FINEST;
			}
			if (this.options.has(RunCommand.this.quiteOption)) {
				return Level.OFF;
			}
			return Level.INFO;
		}

	}
}
