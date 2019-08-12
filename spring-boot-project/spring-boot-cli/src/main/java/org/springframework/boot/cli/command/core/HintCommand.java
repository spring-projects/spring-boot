/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.cli.command.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.boot.cli.command.AbstractCommand;
import org.springframework.boot.cli.command.Command;
import org.springframework.boot.cli.command.CommandRunner;
import org.springframework.boot.cli.command.options.OptionHelp;
import org.springframework.boot.cli.command.status.ExitStatus;
import org.springframework.boot.cli.util.Log;

/**
 * Internal {@link Command} to provide hints for shell auto-completion. Expects to be
 * called with the current index followed by a list of arguments already typed.
 *
 * @author Phillip Webb
 * @since 1.0.0
 */
public class HintCommand extends AbstractCommand {

	private final CommandRunner commandRunner;

	public HintCommand(CommandRunner commandRunner) {
		super("hint", "Provides hints for shell auto-completion");
		this.commandRunner = commandRunner;
	}

	@Override
	public ExitStatus run(String... args) throws Exception {
		try {
			int index = (args.length != 0) ? Integer.valueOf(args[0]) - 1 : 0;
			List<String> arguments = new ArrayList<>(args.length);
			for (int i = 2; i < args.length; i++) {
				arguments.add(args[i]);
			}
			String starting = "";
			if (index < arguments.size()) {
				starting = arguments.remove(index);
			}
			if (index == 0) {
				showCommandHints(starting);
			}
			else if (!arguments.isEmpty() && !starting.isEmpty()) {
				String command = arguments.remove(0);
				showCommandOptionHints(command, Collections.unmodifiableList(arguments), starting);
			}
		}
		catch (Exception ex) {
			// Swallow and provide no hints
			return ExitStatus.ERROR;
		}
		return ExitStatus.OK;
	}

	private void showCommandHints(String starting) {
		for (Command command : this.commandRunner) {
			if (isHintMatch(command, starting)) {
				Log.info(command.getName() + " " + command.getDescription());
			}
		}
	}

	private boolean isHintMatch(Command command, String starting) {
		if (command instanceof HintCommand) {
			return false;
		}
		return command.getName().startsWith(starting)
				|| (this.commandRunner.isOptionCommand(command) && ("--" + command.getName()).startsWith(starting));
	}

	private void showCommandOptionHints(String commandName, List<String> specifiedArguments, String starting) {
		Command command = this.commandRunner.findCommand(commandName);
		if (command != null) {
			for (OptionHelp help : command.getOptionsHelp()) {
				if (!alreadyUsed(help, specifiedArguments)) {
					for (String option : help.getOptions()) {
						if (option.startsWith(starting)) {
							Log.info(option + " " + help.getUsageHelp());
						}
					}
				}
			}
		}
	}

	private boolean alreadyUsed(OptionHelp help, List<String> specifiedArguments) {
		for (String argument : specifiedArguments) {
			if (help.getOptions().contains(argument)) {
				return true;
			}
		}
		return false;
	}

}
