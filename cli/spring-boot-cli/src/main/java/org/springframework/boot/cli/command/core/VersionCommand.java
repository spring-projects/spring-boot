/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.cli.command.core;

import org.springframework.boot.cli.command.AbstractCommand;
import org.springframework.boot.cli.command.Command;
import org.springframework.boot.cli.command.status.ExitStatus;
import org.springframework.boot.cli.util.Log;

/**
 * {@link Command} to display the 'version' number.
 *
 * @author Phillip Webb
 * @since 1.0.0
 */
public class VersionCommand extends AbstractCommand {

	public VersionCommand() {
		super("version", "Show the version");
	}

	@Override
	public ExitStatus run(String... args) {
		Log.info("Spring CLI v" + getClass().getPackage().getImplementationVersion());
		return ExitStatus.OK;
	}

}
