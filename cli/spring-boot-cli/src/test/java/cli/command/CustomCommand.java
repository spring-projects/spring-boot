/*
 * Copyright 2012-present the original author or authors.
 */

package cli.command;

import org.springframework.boot.cli.command.AbstractCommand;
import org.springframework.boot.cli.command.status.ExitStatus;

/**
 * @author Dave Syer
 */
public class CustomCommand extends AbstractCommand {

	public CustomCommand() {
		super("custom", "Custom command added in tests");
	}

	@Override
	public ExitStatus run(String... args) throws Exception {
		System.err.println("Custom Command Hello");
		return ExitStatus.OK;
	}

}
