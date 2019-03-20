/*
 * Copyright 2012-2014 the original author or authors.
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

import jline.console.ConsoleReader;

import org.springframework.boot.cli.command.AbstractCommand;
import org.springframework.boot.cli.command.status.ExitStatus;

/**
 * Clear the {@link Shell} screen.
 *
 * @author Dave Syer
 * @author Phillip Webb
 */
class ClearCommand extends AbstractCommand {

	private final ConsoleReader consoleReader;

	ClearCommand(ConsoleReader consoleReader) {
		super("clear", "Clear the screen");
		this.consoleReader = consoleReader;
	}

	@Override
	public ExitStatus run(String... args) throws Exception {
		this.consoleReader.setPrompt("");
		this.consoleReader.clearScreen();
		return ExitStatus.OK;
	}

}
