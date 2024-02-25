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

package org.springframework.boot.cli.command.shell;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;

import jline.console.ConsoleReader;
import jline.console.completer.CandidateListCompletionHandler;
import org.fusesource.jansi.AnsiRenderer.Code;

import org.springframework.boot.cli.command.Command;
import org.springframework.boot.cli.command.CommandFactory;
import org.springframework.boot.cli.command.CommandRunner;
import org.springframework.boot.cli.command.core.HelpCommand;
import org.springframework.boot.cli.command.core.VersionCommand;
import org.springframework.boot.loader.tools.SignalUtils;
import org.springframework.util.StringUtils;

/**
 * A shell for Spring Boot. Drops the user into an event loop (REPL) where command line
 * completion and history are available without relying on OS shell features.
 *
 * @author Jon Brisbin
 * @author Dave Syer
 * @author Phillip Webb
 * @since 1.0.0
 */
public class Shell {

	private static final Set<Class<?>> NON_FORKED_COMMANDS;

	static {
		Set<Class<?>> nonForked = new HashSet<>();
		nonForked.add(VersionCommand.class);
		NON_FORKED_COMMANDS = Collections.unmodifiableSet(nonForked);
	}

	private final ShellCommandRunner commandRunner;

	private final ConsoleReader consoleReader;

	private final EscapeAwareWhiteSpaceArgumentDelimiter argumentDelimiter = new EscapeAwareWhiteSpaceArgumentDelimiter();

	private final ShellPrompts prompts = new ShellPrompts();

	/**
	 * Create a new {@link Shell} instance.
	 * @throws IOException in case of I/O errors
	 */
	Shell() throws IOException {
		attachSignalHandler();
		this.consoleReader = new ConsoleReader();
		this.commandRunner = createCommandRunner();
		initializeConsoleReader();
	}

	/**
     * Creates a new instance of ShellCommandRunner and configures it with the necessary commands and aliases.
     * 
     * @return The created ShellCommandRunner instance.
     */
    private ShellCommandRunner createCommandRunner() {
		ShellCommandRunner runner = new ShellCommandRunner();
		runner.addCommand(new HelpCommand(runner));
		runner.addCommands(getCommands());
		runner.addAliases("exit", "quit");
		runner.addAliases("help", "?");
		runner.addAliases("clear", "cls");
		return runner;
	}

	/**
     * Retrieves a collection of commands available in the shell.
     * 
     * @return An iterable collection of commands.
     */
    private Iterable<Command> getCommands() {
		List<Command> commands = new ArrayList<>();
		ServiceLoader<CommandFactory> factories = ServiceLoader.load(CommandFactory.class, getClass().getClassLoader());
		for (CommandFactory factory : factories) {
			for (Command command : factory.getCommands()) {
				commands.add(convertToForkCommand(command));
			}
		}
		commands.add(new PromptCommand(this.prompts));
		commands.add(new ClearCommand(this.consoleReader));
		commands.add(new ExitCommand());
		return commands;
	}

	/**
     * Converts the given command to a fork command if it is not already a non-forked command.
     * 
     * @param command the command to be converted
     * @return the converted fork command if the given command is not a non-forked command, otherwise returns the original command
     */
    private Command convertToForkCommand(Command command) {
		for (Class<?> nonForked : NON_FORKED_COMMANDS) {
			if (nonForked.isInstance(command)) {
				return command;
			}
		}
		return new ForkProcessCommand(command);
	}

	/**
     * Initializes the console reader for the Shell class.
     * Enables history, disables bell sound, and disables event expansion.
     * Adds a command completer and sets a completion handler.
     */
    private void initializeConsoleReader() {
		this.consoleReader.setHistoryEnabled(true);
		this.consoleReader.setBellEnabled(false);
		this.consoleReader.setExpandEvents(false);
		this.consoleReader
			.addCompleter(new CommandCompleter(this.consoleReader, this.argumentDelimiter, this.commandRunner));
		this.consoleReader.setCompletionHandler(new CandidateListCompletionHandler());
	}

	/**
     * Attaches a signal handler to handle the SIGINT signal.
     * 
     * @see SignalUtils#attachSignalHandler(SignalHandler)
     * @see #handleSigInt()
     */
    private void attachSignalHandler() {
		SignalUtils.attachSignalHandler(this::handleSigInt);
	}

	/**
	 * Run the shell until the user exists.
	 * @throws Exception on error
	 */
	public void run() throws Exception {
		printBanner();
		try {
			runInputLoop();
		}
		catch (Exception ex) {
			if (!(ex instanceof ShellExitException)) {
				throw ex;
			}
		}
	}

	/**
     * Prints the banner for the Shell class.
     * The banner includes the version of the class if available.
     * The banner also provides instructions for using the Shell class.
     * 
     * @see Shell
     * @since 1.0.0
     */
    private void printBanner() {
		String version = getClass().getPackage().getImplementationVersion();
		version = (version != null) ? " (v" + version + ")" : "";
		System.out.println(ansi("Spring Boot", Code.BOLD).append(version, Code.FAINT));
		System.out.println(ansi("Hit TAB to complete. Type 'help' and hit RETURN for help, and 'exit' to quit."));
	}

	/**
     * Runs the input loop for the shell.
     * 
     * @throws Exception if an error occurs during the input loop
     */
    private void runInputLoop() throws Exception {
		String line;
		while ((line = this.consoleReader.readLine(getPrompt())) != null) {
			while (line.endsWith("\\")) {
				line = line.substring(0, line.length() - 1);
				line += this.consoleReader.readLine("> ");
			}
			if (StringUtils.hasLength(line)) {
				String[] args = this.argumentDelimiter.parseArguments(line);
				this.commandRunner.runAndHandleErrors(args);
			}
		}
	}

	/**
     * Returns the prompt for the Shell.
     * 
     * @return the prompt for the Shell
     */
    private String getPrompt() {
		String prompt = this.prompts.getPrompt();
		return ansi(prompt, Code.FG_BLUE).toString();
	}

	/**
     * Returns an AnsiString object with the specified text and codes.
     * 
     * @param text the text to be appended to the AnsiString object
     * @param codes the codes to be applied to the AnsiString object
     * @return an AnsiString object with the specified text and codes
     */
    private AnsiString ansi(String text, Code... codes) {
		return new AnsiString(this.consoleReader.getTerminal()).append(text, codes);
	}

	/**
	 * Final handle an interrupt signal (CTRL-C).
	 */
	protected void handleSigInt() {
		if (this.commandRunner.handleSigInt()) {
			return;
		}
		System.out.println(String.format("%nThanks for using Spring Boot"));
		System.exit(1);
	}

	/**
	 * Extension of {@link CommandRunner} to deal with {@link RunProcessCommand}s and
	 * aliases.
	 */
	private class ShellCommandRunner extends CommandRunner {

		private volatile Command lastCommand;

		private final Map<String, String> aliases = new HashMap<>();

		/**
         * Constructs a new ShellCommandRunner with a null command.
         */
        ShellCommandRunner() {
			super(null);
		}

		/**
         * Adds aliases for a command.
         * 
         * @param command the command to add aliases for
         * @param aliases the aliases to be added for the command
         */
        void addAliases(String command, String... aliases) {
			for (String alias : aliases) {
				this.aliases.put(alias, command);
			}
		}

		/**
         * Finds a command based on the given name.
         * 
         * @param name the name of the command to find
         * @return the command object corresponding to the given name
         */
        @Override
		public Command findCommand(String name) {
			if (name.startsWith("!")) {
				return new RunProcessCommand(name.substring(1));
			}
			if (this.aliases.containsKey(name)) {
				name = this.aliases.get(name);
			}
			return super.findCommand(name);
		}

		/**
         * Sets the last command executed before running the command.
         * 
         * @param command the command to be executed
         */
        @Override
		protected void beforeRun(Command command) {
			this.lastCommand = command;
		}

		/**
         * This method is called after the execution of a command in the ShellCommandRunner class.
         * It can be overridden in subclasses to perform additional actions or handle the command result.
         * 
         * @param command The command that was executed.
         */
        @Override
		protected void afterRun(Command command) {
		}

		/**
         * Handles the SIGINT signal.
         * 
         * @return true if the SIGINT signal was successfully handled, false otherwise.
         */
        boolean handleSigInt() {
			Command command = this.lastCommand;
			if (command instanceof RunProcessCommand runProcessCommand) {
				return runProcessCommand.handleSigInt();
			}
			return false;
		}

	}

}
