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
 * {@link Command} to start a nested REPL shell.
 *
 * @author Phillip Webb
 * @since 1.0.0
 * @see Shell
 */
public class ShellCommand extends AbstractCommand {

	/**
     * Constructs a new ShellCommand object.
     * This command is used to start a nested shell.
     */
    public ShellCommand() {
		super("shell", "Start a nested shell");
	}

	/**
     * Executes the run method of the ShellCommand class.
     * 
     * @param args the command line arguments
     * @return the exit status of the run method
     * @throws Exception if an error occurs during execution
     */
    @Override
	public ExitStatus run(String... args) throws Exception {
		new Shell().run();
		return ExitStatus.OK;
	}

}
