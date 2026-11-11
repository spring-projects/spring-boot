/*
 * Copyright 2012-present the original author or authors.
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
