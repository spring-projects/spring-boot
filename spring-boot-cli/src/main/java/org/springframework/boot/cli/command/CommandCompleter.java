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

package org.springframework.boot.cli.command;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jline.console.ConsoleReader;
import jline.console.completer.ArgumentCompleter;
import jline.console.completer.Completer;
import jline.console.completer.NullCompleter;
import jline.console.completer.StringsCompleter;

import org.springframework.boot.cli.Command;
import org.springframework.boot.cli.Log;
import org.springframework.boot.cli.OptionHelp;
import org.springframework.boot.cli.SpringCli;

/**
 * JLine {@link Completer} for Spring Boot {@link Command}s.
 * 
 * @author Jon Brisbin
 * @author Phillip Webb
 */
public class CommandCompleter extends StringsCompleter {

	private final Map<String, Completer> optionCompleters = new HashMap<String, Completer>();

	private List<Command> commands = new ArrayList<Command>();

	private ConsoleReader console;

	public CommandCompleter(ConsoleReader consoleReader, SpringCli cli) {
		this.console = consoleReader;
		this.commands.addAll(cli.getCommands());
		List<String> names = new ArrayList<String>();
		for (Command command : this.commands) {
			names.add(command.getName());
			List<String> options = new ArrayList<String>();
			for (OptionHelp optionHelp : command.getOptionsHelp()) {
				options.addAll(optionHelp.getOptions());
			}
			StringsCompleter commandCompleter = new StringsCompleter(command.getName());
			StringsCompleter optionsCompleter = new StringsCompleter(options);
			this.optionCompleters.put(command.getName(), new ArgumentCompleter(
					commandCompleter, optionsCompleter, new NullCompleter()));
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
					Completer completer = this.optionCompleters.get(command.getName());
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
		catch (IOException e) {
			Log.error(e.getMessage() + " (" + e.getClass().getName() + ")");
		}
	}

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
