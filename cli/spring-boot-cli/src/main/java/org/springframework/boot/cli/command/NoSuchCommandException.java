/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.cli.command;

/**
 * Exception used when a command is not found.
 *
 * @author Phillip Webb
 * @since 1.0.0
 */
public class NoSuchCommandException extends CommandException {

	private static final long serialVersionUID = 1L;

	public NoSuchCommandException(String name) {
		super(String.format("'%1$s' is not a valid command. See 'help'.", name));
	}

}
