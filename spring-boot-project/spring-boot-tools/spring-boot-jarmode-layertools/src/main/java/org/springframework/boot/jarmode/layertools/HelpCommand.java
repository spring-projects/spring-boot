/*
 * Copyright 2012-2023 the original author or authors.
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

	/**
     * Constructs a new HelpCommand with the specified context and list of commands.
     * 
     * @param context the context in which the command is executed
     * @param commands the list of commands available
     */
    HelpCommand(Context context, List<Command> commands) {
		super("help", "Help about any command", Options.none(), Parameters.of("[<command]"));
		this.context = context;
		this.commands = commands;
	}

	/**
     * Runs the HelpCommand with the given options and parameters.
     * 
     * @param options     a map of options for the HelpCommand
     * @param parameters  a list of parameters for the HelpCommand
     */
    @Override
	protected void run(Map<Option, String> options, List<String> parameters) {
		run(System.out, parameters);
	}

	/**
     * Runs the help command.
     * 
     * @param out the PrintStream to output the help information
     * @param parameters the list of parameters passed to the help command
     */
    void run(PrintStream out, List<String> parameters) {
		Command command = (!parameters.isEmpty()) ? Command.find(this.commands, parameters.get(0)) : null;
		if (command != null) {
			printCommandHelp(out, command);
			return;
		}
		printUsageAndCommands(out);
	}

	/**
     * Prints the help information for a specific command.
     * 
     * @param out the output stream to print the help information to
     * @param command the command to get the help information for
     */
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

	/**
     * Prints the summary of an option.
     *
     * @param out the output stream to print the summary to
     * @param option the option to print the summary for
     * @param padding the padding for the option name and value description
     */
    private void printOptionSummary(PrintStream out, Option option, int padding) {
		out.printf("  --%-" + padding + "s  %s%n", option.getNameAndValueDescription(), option.getDescription());
	}

	/**
     * Returns the usage string for the given command.
     *
     * @param command the command for which to generate the usage string
     * @return the usage string for the given command
     */
    private String getUsage(Command command) {
		StringBuilder usage = new StringBuilder();
		usage.append(command.getName());
		if (!command.getOptions().isEmpty()) {
			usage.append(" [options]");
		}
		command.getParameters().getDescriptions().forEach((param) -> usage.append(" ").append(param));
		return usage.toString();
	}

	/**
     * Prints the usage and available commands.
     * 
     * @param out the PrintStream to output the usage and commands
     */
    private void printUsageAndCommands(PrintStream out) {
		out.println("Usage:");
		out.println("  " + getJavaCommand());
		out.println();
		out.println("Available commands:");
		int maxNameLength = getMaxLength(getName().length(), this.commands.stream().map(Command::getName));
		this.commands.forEach((command) -> printCommandSummary(out, command, maxNameLength));
		printCommandSummary(out, this, maxNameLength);
	}

	/**
     * Returns the maximum length of strings in the given stream, considering the minimum length as well.
     *
     * @param minimum the minimum length to consider
     * @param strings the stream of strings to be evaluated
     * @return the maximum length of strings in the stream, considering the minimum length
     */
    private int getMaxLength(int minimum, Stream<String> strings) {
		return Math.max(minimum, strings.mapToInt(String::length).max().orElse(0));
	}

	/**
     * Prints the summary of a command.
     *
     * @param out     the output stream to print the summary to
     * @param command the command to print the summary for
     * @param padding the padding for formatting the output
     */
    private void printCommandSummary(PrintStream out, Command command, int padding) {
		out.printf("  %-" + padding + "s  %s%n", command.getName(), command.getDescription());
	}

	/**
     * Returns the command to execute the Java application.
     * The command includes the necessary options and the name of the archive file.
     * 
     * @return the Java command to execute the application
     */
    private String getJavaCommand() {
		return "java -Djarmode=layertools -jar " + this.context.getArchiveFile().getName();
	}

}
