/*
 * Copyright 2012-present the original author or authors.
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

	public ShellCommand() {
		super("shell", "Start a nested shell");
	}

	@Override
	public ExitStatus run(String... args) throws Exception {
		new Shell().run();
		return ExitStatus.OK;
	}

}
