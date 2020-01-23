/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.jarmode.layertools;

import java.io.PrintStream;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Implicit {@code 'help'} command.
 *
 * @author Phillip Webb
 */
class HelpCommand extends Command {

	private final Context context;

	private final List<Command> commands;

	HelpCommand(Context context, List<Command> commands) {
		super("help", "Help about any command", Options.none(), Parameters.of("[<command]"));
		this.context = context;
		this.commands = commands;
	}

	@Override
	protected void run(Map<Option, String> options, List<String> parameters) {
		run(System.out, options, parameters);
	}

	void run(PrintStream out, Map<Option, String> options, List<String> parameters) {
		Command command = (!parameters.isEmpty()) ? Command.find(this.commands, parameters.get(0)) : null;
		if (command != null) {
			printCommandHelp(out, command);
			return;
		}
		printUsageAndCommands(out);
	}

	private void printCommandHelp(PrintStream out, Command command) {
		out.println(command.getDescription());
		out.println();
		out.println("Usage:");
		out.println("  " + getJavaCommand() + " " + getUsage(command));
		if (!command.getOptions().isEmpty()) {
			out.println();
			out.println("Options:");
			int maxNameLength = getMaxLength(0, command.getOptions().stream().map(Option::getNameAndValueDescription));
			command.getOptions().stream().forEach((option) -> printOptionSummary(out, option, maxNameLength));
		}
	}

	private void printOptionSummary(PrintStream out, Option option, int padding) {
		out.println(String.format("  --%-" + padding + "s  %s", option.getNameAndValueDescription(),
				option.getDescription()));
	}

	private String getUsage(Command command) {
		StringBuilder usage = new StringBuilder();
		usage.append(command.getName());
		if (!command.getOptions().isEmpty()) {
			usage.append(" [options]");
		}
		command.getParameters().getDescriptions().forEach((param) -> usage.append(" " + param));
		return usage.toString();
	}

	private void printUsageAndCommands(PrintStream out) {
		out.println("Usage:");
		out.println("  " + getJavaCommand());
		out.println();
		out.println("Available commands:");
		int maxNameLength = getMaxLength(getName().length(), this.commands.stream().map(Command::getName));
		this.commands.forEach((command) -> printCommandSummary(out, command, maxNameLength));
		printCommandSummary(out, this, maxNameLength);
	}

	private int getMaxLength(int minimum, Stream<String> strings) {
		return Math.max(minimum, strings.mapToInt(String::length).max().orElse(0));
	}

	private void printCommandSummary(PrintStream out, Command command, int padding) {
		out.println(String.format("  %-" + padding + "s  %s", command.getName(), command.getDescription()));
	}

	private String getJavaCommand() {
		return "java -Djarmode=layertools -jar " + this.context.getJarFile().getName();
	}

}
