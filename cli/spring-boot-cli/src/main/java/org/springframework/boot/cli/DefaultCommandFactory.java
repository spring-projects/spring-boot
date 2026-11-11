/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.cli;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.springframework.boot.cli.command.Command;
import org.springframework.boot.cli.command.CommandFactory;
import org.springframework.boot.cli.command.core.VersionCommand;
import org.springframework.boot.cli.command.encodepassword.EncodePasswordCommand;
import org.springframework.boot.cli.command.init.InitCommand;

/**
 * Default implementation of {@link CommandFactory}.
 *
 * @author Dave Syer
 * @since 1.0.0
 */
public class DefaultCommandFactory implements CommandFactory {

	private static final List<Command> DEFAULT_COMMANDS;

	static {
		List<Command> defaultCommands = new ArrayList<>();
		defaultCommands.add(new VersionCommand());
		defaultCommands.add(new InitCommand());
		defaultCommands.add(new EncodePasswordCommand());
		DEFAULT_COMMANDS = Collections.unmodifiableList(defaultCommands);
	}

	@Override
	public Collection<Command> getCommands() {
		return DEFAULT_COMMANDS;
	}

}
