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

	@Override
	public boolean accepts(String mode) {
		return "layertools".equalsIgnoreCase(mode);
	}

	@Override
	public void run(String mode, String[] args) {
		try {
			new Runner().run(args);
		}
		catch (Exception ex) {
			throw new IllegalStateException(ex);
		}
	}

	static class Runner {

		static Context contextOverride;

		private final List<Command> commands;

		private final HelpCommand help;

		Runner() {
			Context context = (contextOverride != null) ? contextOverride : new Context();
			this.commands = getCommands(context);
			this.help = new HelpCommand(context, this.commands);
		}

		private void run(String[] args) {
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
			this.help.run(args);
		}

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

		private void printError(String errorMessage) {
			System.out.println("Error: " + errorMessage);
			System.out.println();
		}

		private Deque<String> dequeOf(String... args) {
			return new ArrayDeque<>(Arrays.asList(args));
		}

		static List<Command> getCommands(Context context) {
			List<Command> commands = new ArrayList<>();
			commands.add(new ListCommand(context));
			commands.add(new ExtractCommand(context));
			return Collections.unmodifiableList(commands);
		}

	}

}
