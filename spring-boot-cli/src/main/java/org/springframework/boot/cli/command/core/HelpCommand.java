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

package org.springframework.boot.cli.command.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.springframework.boot.cli.command.AbstractCommand;
import org.springframework.boot.cli.command.Command;
import org.springframework.boot.cli.command.CommandRunner;
import org.springframework.boot.cli.command.NoHelpCommandArgumentsException;
import org.springframework.boot.cli.command.NoSuchCommandException;
import org.springframework.boot.cli.command.options.OptionHelp;
import org.springframework.boot.cli.command.status.ExitStatus;
import org.springframework.boot.cli.util.Log;

/**
 * Internal {@link Command} used for 'help' requests.
 * 
 * @author Phillip Webb
 */
public class HelpCommand extends AbstractCommand {

	private final CommandRunner commandRunner;

	public HelpCommand(CommandRunner commandRunner) {
		super("help", "Get help on commands");
		this.commandRunner = commandRunner;
	}

	@Override
	public String getUsageHelp() {
		return "command";
	}

	@Override
	public String getHelp() {
		return null;
	}

	@Override
	public Collection<OptionHelp> getOptionsHelp() {
		List<OptionHelp> help = new ArrayList<OptionHelp>();
		for (final Command command : this.commandRunner) {
			if (isHelpShown(command)) {
				help.add(new OptionHelp() {

					@Override
					public Set<String> getOptions() {
						return Collections.singleton(command.getName());
					}

					@Override
					public String getUsageHelp() {
						return command.getDescription();
					}

				});
			}
		}
		return help;
	}

	private boolean isHelpShown(Command command) {
		if (command instanceof HelpCommand || command instanceof HintCommand) {
			return false;
		}
		return true;
	}

	@Override
	public ExitStatus run(String... args) throws Exception {
		if (args.length == 0) {
			throw new NoHelpCommandArgumentsException();
		}
		String commandName = args[0];
		for (Command command : this.commandRunner) {
			if (command.getName().equals(commandName)) {
				Log.info(this.commandRunner.getName() + command.getName() + " - "
						+ command.getDescription());
				Log.info("");
				if (command.getUsageHelp() != null) {
					Log.info("usage: " + this.commandRunner.getName() + command.getName()
							+ " " + command.getUsageHelp());
					Log.info("");
				}
				if (command.getHelp() != null) {
					Log.info(command.getHelp());
				}
				return ExitStatus.OK;
			}
		}
		throw new NoSuchCommandException(commandName);
	}

}
