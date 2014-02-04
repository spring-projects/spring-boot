/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.cli.command.shell;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jline.console.ConsoleReader;
import jline.console.completer.AggregateCompleter;
import jline.console.completer.ArgumentCompleter;
import jline.console.completer.ArgumentCompleter.ArgumentDelimiter;
import jline.console.completer.Completer;
import jline.console.completer.FileNameCompleter;
import jline.console.completer.StringsCompleter;

import org.springframework.boot.cli.command.Command;
import org.springframework.boot.cli.command.options.OptionHelp;
import org.springframework.boot.cli.util.Log;

/**
 * JLine {@link Completer} for Spring Boot {@link Command}s.
 * 
 * @author Jon Brisbin
 * @author Phillip Webb
 */
public class CommandCompleter extends StringsCompleter {

	private final Map<String, Completer> commandCompleters = new HashMap<String, Completer>();

	private final List<Command> commands = new ArrayList<Command>();

	private final ConsoleReader console;

	public CommandCompleter(ConsoleReader consoleReader,
			ArgumentDelimiter argumentDelimiter, Iterable<Command> commands) {
		this.console = consoleReader;
		List<String> names = new ArrayList<String>();
		for (Command command : commands) {
			this.commands.add(command);
			names.add(command.getName());
			List<String> options = new ArrayList<String>();
			for (OptionHelp optionHelp : command.getOptionsHelp()) {
				options.addAll(optionHelp.getOptions());
			}
			AggregateCompleter arguementCompleters = new AggregateCompleter(
					new StringsCompleter(options), new FileNameCompleter());
			ArgumentCompleter argumentCompleter = new ArgumentCompleter(
					argumentDelimiter, arguementCompleters);
			argumentCompleter.setStrict(false);
			this.commandCompleters.put(command.getName(), argumentCompleter);
		}
		getStrings().addAll(names);
	}

	@Override
	public int complete(String buffer, int cursor, List<CharSequence> candidates) {
		int completionIndex = super.complete(buffer, cursor, candidates);
		int spaceIndex = buffer.indexOf(' ');
		String commandName = (spaceIndex == -1) ? "" : buffer.substring(0, spaceIndex);
		if (!"".equals(commandName.trim())) {
			for (Command command : this.commands) {
				if (command.getName().equals(commandName)) {
					if (cursor == buffer.length() && buffer.endsWith(" ")) {
						printUsage(command);
						break;
					}
					Completer completer = this.commandCompleters.get(command.getName());
					if (completer != null) {
						completionIndex = completer.complete(buffer, cursor, candidates);
						break;
					}
				}
			}
		}
		return completionIndex;
	}

	private void printUsage(Command command) {
		try {
			int maxOptionsLength = 0;
			List<OptionHelpLine> optionHelpLines = new ArrayList<OptionHelpLine>();
			for (OptionHelp optionHelp : command.getOptionsHelp()) {
				OptionHelpLine optionHelpLine = new OptionHelpLine(optionHelp);
				optionHelpLines.add(optionHelpLine);
				maxOptionsLength = Math.max(maxOptionsLength, optionHelpLine.getOptions()
						.length());
			}

			this.console.println();
			this.console.println("Usage:");
			this.console.println(command.getName() + " " + command.getUsageHelp());
			for (OptionHelpLine optionHelpLine : optionHelpLines) {
				this.console.println(String.format("\t%" + maxOptionsLength + "s: %s",
						optionHelpLine.getOptions(), optionHelpLine.getUsage()));
			}
			this.console.drawLine();
		}
		catch (IOException ex) {
			Log.error(ex.getMessage() + " (" + ex.getClass().getName() + ")");
		}
	}

	/**
	 * Encapsulated options and usage help.
	 */
	private static class OptionHelpLine {

		private final String options;

		private final String usage;

		public OptionHelpLine(OptionHelp optionHelp) {
			StringBuffer options = new StringBuffer();
			for (String option : optionHelp.getOptions()) {
				options.append(options.length() == 0 ? "" : ", ");
				options.append(option);
			}
			this.options = options.toString();
			this.usage = optionHelp.getUsageHelp();
		}

		public String getOptions() {
			return this.options;
		}

		public String getUsage() {
			return this.usage;
		}
	}
}
