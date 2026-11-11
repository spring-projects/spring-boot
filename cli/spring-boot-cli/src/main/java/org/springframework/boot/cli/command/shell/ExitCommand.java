/*
 * Copyright 2012-present the original author or authors.
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

	ExitCommand() {
		super("exit", "Quit the embedded shell");
	}

	@Override
	public ExitStatus run(String... args) throws Exception {
		throw new ShellExitException();
	}

}
