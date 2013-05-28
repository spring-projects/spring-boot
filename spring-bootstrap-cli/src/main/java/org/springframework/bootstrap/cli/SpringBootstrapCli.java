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

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;

/**
 * Spring Bootstrap Command Line Interface. This is the main entry-point for the spring
 * bootstrap command line application. This class will parse input arguments and delegate
 * to a suitable {@link Command} implementation based on the first argument.
 * 
 * <p>
 * The '-d' and '--debug' switches are handled by this class, however, most argument
 * parsing is left to the {@link Command} implementation.
 * 
 * @author Phillip Webb
 * @see #main(String...)
 * @see BootstrapCliException
 * @see Command
 */
public class SpringBootstrapCli {

	public static final String CLI_APP = "spring";

	private static final Set<BootstrapCliException.Option> NO_EXCEPTION_OPTIONS = EnumSet
			.noneOf(BootstrapCliException.Option.class);

	private List<Command> commands;

	/**
	 * Create a new {@link SpringBootstrapCli} implementation with the default set of
	 * commands.
	 */
	public SpringBootstrapCli() {
		setCommands(ServiceLoader.load(CommandFactory.class, getClass().getClassLoader()));
	}

	private void setCommands(Iterable<CommandFactory> iterable) {
		this.commands = new ArrayList<Command>();
		for (CommandFactory factory : iterable) {
			for (Command command : factory.getCommands()) {
				this.commands.add(command);
			}
		}
		this.commands.add(0, new HelpOptionCommand());
		this.commands.add(0, new HelpCommand());
	}

	/**
	 * Set the command available to the CLI. Primarily used to support testing. NOTE: The
	 * 'help' command will be automatically provided in addition to this list.
	 * @param commands the commands to add
	 */
	public void setCommands(List<? extends Command> commands) {
		this.commands = new ArrayList<Command>(commands);
		this.commands.add(0, new HelpOptionCommand());
		this.commands.add(0, new HelpCommand());
	}

	/**
	 * Run the CLI and handle and errors.
	 * @param args the input arguments
	 * @return a return status code (non zero is used to indicate an error)
	 */
	public int runAndHandleErrors(String... args) {
		String[] argsWithoutDebugFlags = removeDebugFlags(args);
		boolean debug = argsWithoutDebugFlags.length != args.length;
		try {
			run(argsWithoutDebugFlags);
			return 0;
		} catch (NoArgumentsException ex) {
			showUsage();
			return 1;
		} catch (Exception ex) {
			Set<BootstrapCliException.Option> options = NO_EXCEPTION_OPTIONS;
			if (ex instanceof BootstrapCliException) {
				options = ((BootstrapCliException) ex).getOptions();
			}
			if (!(ex instanceof NoHelpCommandArgumentsException)) {
				errorMessage(ex.getMessage());
			}
			if (options.contains(BootstrapCliException.Option.SHOW_USAGE)) {
				showUsage();
			}
			if (debug || options.contains(BootstrapCliException.Option.STACK_TRACE)) {
				printStackTrace(ex);
			}
			return 1;
		}
	}

	/**
	 * Parse the arguments and run a suitable command.
	 * @param args the arguments
	 * @throws Exception
	 */
	protected void run(String... args) throws Exception {
		if (args.length == 0) {
			throw new NoArgumentsException();
		}
		String commandName = args[0];
		String[] commandArguments = Arrays.copyOfRange(args, 1, args.length);
		find(commandName).run(commandArguments);
	}

	private Command find(String name) {
		for (Command candidate : this.commands) {
			if (candidate.getName().equals(name)) {
				return candidate;
			}
		}
		throw new NoSuchCommandException(name);
	}

	protected void showUsage() {
		System.out.print("usage: " + CLI_APP + " ");
		System.out.println("");
		System.out.println("       <command> [<args>]");
		System.out.println("");
		System.out.println("Available commands are:");
		for (Command command : this.commands) {
			System.out.println(String.format("\n  %1$s %2$-15s\n    %3$s",
					command.getName(), command.getUsageHelp(), command.getDescription()));
		}
		System.out.println("");
		System.out.println("See '" + CLI_APP
				+ " help <command>' for more information on a specific command.");
	}

	protected void errorMessage(String message) {
		System.err.println(message == null ? "Unexpected error" : message);
	}

	protected void printStackTrace(Exception ex) {
		System.err.println("");
		ex.printStackTrace(System.err);
		System.err.println("");
	}

	private String[] removeDebugFlags(String[] args) {
		List<String> rtn = new ArrayList<String>(args.length);
		for (String arg : args) {
			if (!("-d".equals(arg) || "--debug".equals(arg))) {
				rtn.add(arg);
			}
		}
		return rtn.toArray(new String[rtn.size()]);
	}

	private class HelpOptionCommand extends HelpCommand {
		@Override
		public String getName() {
			return "--help";
		}
	}

	/**
	 * Internal {@link Command} used for 'help' and '--help' requests.
	 */
	private class HelpCommand implements Command {

		@Override
		public void run(String... args) throws Exception {
			if (args.length == 0) {
				throw new NoHelpCommandArgumentsException();
			}
			String commandName = args[0];
			for (Command command : SpringBootstrapCli.this.commands) {
				if (command.getName().equals(commandName)) {
					System.out.println(CLI_APP + " " + command.getName() + " - "
							+ command.getDescription());
					System.out.println();
					if (command.getUsageHelp() != null) {
						System.out.println("usage: " + CLI_APP + " " + command.getName()
								+ " " + command.getUsageHelp());
						System.out.println();
					}
					command.printHelp(System.out);
					return;
				}
			}
			throw new NoSuchCommandException(commandName);
		}

		@Override
		public String getName() {
			return "help";
		}

		@Override
		public String getDescription() {
			return "Get help on commands";
		}

		@Override
		public String getUsageHelp() {
			return null;
		}

		@Override
		public void printHelp(PrintStream out) throws IOException {
		}

	}

	static class NoHelpCommandArgumentsException extends BootstrapCliException {

		private static final long serialVersionUID = 1L;

		public NoHelpCommandArgumentsException() {
			super(Option.SHOW_USAGE);
		}

	}

	static class NoArgumentsException extends BootstrapCliException {

		private static final long serialVersionUID = 1L;

	}

	/**
	 * The main CLI entry-point.
	 * @param args CLI arguments
	 */
	public static void main(String... args) {
		int exitCode = new SpringBootstrapCli().runAndHandleErrors(args);
		if (exitCode != 0) {
			System.exit(exitCode);
		}
	}
}
