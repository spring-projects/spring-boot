/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.cli.command.shell;

import org.springframework.boot.cli.command.CommandException;

/**
 * Exception used to stop the {@link Shell}.
 *
 * @author Phillip Webb
 * @since 1.0.0
 */
public class ShellExitException extends CommandException {

	private static final long serialVersionUID = 1L;

	public ShellExitException() {
		super(Option.RETHROW);
	}

}
