/*
 * Copyright 2012-2022 the original author or authors.
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

import org.springframework.boot.cli.command.AbstractCommand;
import org.springframework.boot.cli.command.Command;
import org.springframework.boot.cli.command.status.ExitStatus;

/**
 * {@link Command} to quit the {@link Shell}.
 *
 * @author Phillip Webb
 */
class ExitCommand extends AbstractCommand {

	/**
	 * Creates a new ExitCommand object with the specified command name and description.
	 * @param commandName the name of the command
	 * @param description the description of the command
	 */
	ExitCommand() {
		super("exit", "Quit the embedded shell");
	}

	/**
	 * Executes the command and returns the exit status.
	 * @param args the command arguments
	 * @return the exit status
	 * @throws Exception if an error occurs during command execution
	 * @throws ShellExitException if the command execution results in a shell exit
	 */
	@Override
	public ExitStatus run(String... args) throws Exception {
		throw new ShellExitException();
	}

}
