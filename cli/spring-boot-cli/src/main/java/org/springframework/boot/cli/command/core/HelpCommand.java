/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.cli.command.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.cli.command.AbstractCommand;
import org.springframework.boot.cli.command.Command;
import org.springframework.boot.cli.command.CommandRunner;
import org.springframework.boot.cli.command.HelpExample;
import org.springframework.boot.cli.command.NoHelpCommandArgumentsException;
import org.springframework.boot.cli.command.NoSuchCommandException;
import org.springframework.boot.cli.command.options.OptionHelp;
import org.springframework.boot.cli.command.status.ExitStatus;
import org.springframework.boot.cli.util.Log;

/**
 * Internal {@link Command} used for 'help' requests.
 *
 * @author Phillip Webb
 * @since 1.0.0
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
	public @Nullable String getHelp() {
		return null;
	}

	@Override
	public Collection<OptionHelp> getOptionsHelp() {
		List<OptionHelp> help = new ArrayList<>();
		for (Command command : this.commandRunner) {
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
		return !(command instanceof HelpCommand) && !(command instanceof HintCommand);
	}

	@Override
	public ExitStatus run(String... args) throws Exception {
		if (args.length == 0) {
			throw new NoHelpCommandArgumentsException();
		}
		String commandName = args[0];
		for (Command command : this.commandRunner) {
			if (command.getName().equals(commandName)) {
				Log.info(this.commandRunner.getName() + command.getName() + " - " + command.getDescription());
				Log.info("");
				if (command.getUsageHelp() != null) {
					Log.info("usage: " + this.commandRunner.getName() + command.getName() + " "
							+ command.getUsageHelp());
					Log.info("");
				}
				if (command.getHelp() != null) {
					Log.info(command.getHelp());
				}
				Collection<HelpExample> examples = command.getExamples();
				if (examples != null) {
					Log.info((examples.size() != 1) ? "examples:" : "example:");
					Log.info("");
					for (HelpExample example : examples) {
						Log.info("    " + example.getDescription() + ":");
						Log.info("        $ " + example.getExample());
						Log.info("");
					}
					Log.info("");
				}
				return ExitStatus.OK;
			}
		}
		throw new NoSuchCommandException(commandName);
	}

}
