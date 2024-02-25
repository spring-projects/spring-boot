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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

import org.springframework.boot.loader.jarmode.JarMode;

/**
 * {@link JarMode} providing {@code "layertools"} support.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 * @since 2.3.0
 */
public class LayerToolsJarMode implements JarMode {

	/**
	 * Determines if the given mode is accepted by the LayerToolsJarMode class.
	 * @param mode the mode to be checked
	 * @return true if the mode is accepted, false otherwise
	 */
	@Override
	public boolean accepts(String mode) {
		return "layertools".equalsIgnoreCase(mode);
	}

	/**
	 * Runs the specified mode with the given arguments.
	 * @param mode the mode to run
	 * @param args the arguments for the mode
	 * @throws IllegalStateException if an exception occurs during the execution of the
	 * mode
	 */
	@Override
	public void run(String mode, String[] args) {
		try {
			new Runner().run(args);
		}
		catch (Exception ex) {
			throw new IllegalStateException(ex);
		}
	}

	/**
	 * Runner class.
	 */
	static class Runner {

		static Context contextOverride;

		private final List<Command> commands;

		private final HelpCommand help;

		/**
		 * Initializes a new instance of the Runner class.
		 * @param contextOverride The optional context to be used. If null, a new Context
		 * will be created.
		 */
		Runner() {
			Context context = (contextOverride != null) ? contextOverride : new Context();
			this.commands = getCommands(context);
			this.help = new HelpCommand(context, this.commands);
		}

		/**
		 * Runs the program with the given command line arguments.
		 * @param args the command line arguments as an array of strings
		 */
		private void run(String[] args) {
			run(dequeOf(args));
		}

		/**
		 * Runs the specified command with the given arguments.
		 * @param args the arguments for the command
		 */
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
			this.help.run(args);
		}

		/**
		 * Runs the given command with the provided arguments.
		 * @param command the command to be executed
		 * @param args the arguments for the command
		 */
		private void runCommand(Command command, Deque<String> args) {
			try {
				command.run(args);
			}
			catch (UnknownOptionException ex) {
				printError("Unknown option \"" + ex.getMessage() + "\" for the " + command.getName() + " command");
				this.help.run(dequeOf(command.getName()));
			}
			catch (MissingValueException ex) {
				printError("Option \"" + ex.getMessage() + "\" for the " + command.getName()
						+ " command requires a value");
				this.help.run(dequeOf(command.getName()));
			}
		}

		/**
		 * Prints an error message to the console.
		 * @param errorMessage the error message to be printed
		 */
		private void printError(String errorMessage) {
			System.out.println("Error: " + errorMessage);
			System.out.println();
		}

		/**
		 * Creates a new Deque of Strings using the provided arguments.
		 * @param args the Strings to be added to the Deque
		 * @return a Deque of Strings containing the provided arguments
		 */
		private Deque<String> dequeOf(String... args) {
			return new ArrayDeque<>(Arrays.asList(args));
		}

		/**
		 * Returns a list of commands available in the given context.
		 * @param context the context in which the commands are available
		 * @return a list of commands
		 */
		static List<Command> getCommands(Context context) {
			List<Command> commands = new ArrayList<>();
			commands.add(new ListCommand(context));
			commands.add(new ExtractCommand(context));
			return Collections.unmodifiableList(commands);
		}

	}

}
