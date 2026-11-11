/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jarmode.tools;

import java.io.PrintStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;

/**
 * Runs commands.
 *
 * @author Moritz Halbritter
 */
class Runner {

	private final PrintStream out;

	private final List<Command> commands = new ArrayList<>();

	private final HelpCommand help;

	Runner(PrintStream out, Context context, List<Command> commands) {
		this.out = out;
		this.commands.addAll(commands);
		this.help = new HelpCommand(context, commands);
		this.commands.add(this.help);
	}

	void run(String... args) {
		run(dequeOf(args));
	}

	private void run(Deque<String> args) {
		if (!args.isEmpty()) {
			String commandName = args.removeFirst();
			Command command = Command.find(this.commands, commandName);
			if (command != null) {
				runCommand(command, args);
				return;
			}
			printError("Unknown command \"" + commandName + "\"");
		}
		this.help.run(this.out, args);
	}

	private void runCommand(Command command, Deque<String> args) {
		if (command.isDeprecated()) {
			printWarning("This command is deprecated. " + command.getDeprecationMessage());
		}
		try {
			command.run(this.out, args);
		}
		catch (UnknownOptionException ex) {
			printError("Unknown option \"" + ex.getMessage() + "\" for the " + command.getName() + " command");
			this.help.printCommandHelp(this.out, command, false);
		}
		catch (MissingValueException ex) {
			printError("Option \"" + ex.getMessage() + "\" for the " + command.getName() + " command requires a value");
			this.help.printCommandHelp(this.out, command, false);
		}
	}

	private void printWarning(String message) {
		this.out.println("Warning: " + message);
		this.out.println();
	}

	private void printError(String message) {
		this.out.println("Error: " + message);
		this.out.println();
	}

	private Deque<String> dequeOf(String... args) {
		return new ArrayDeque<>(Arrays.asList(args));
	}

}
