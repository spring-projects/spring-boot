/*
 * Copyright 2012-present the original author or authors.
 */

package cli.command;

import java.util.Collection;
import java.util.Collections;

import org.springframework.boot.cli.command.Command;
import org.springframework.boot.cli.command.CommandFactory;

/**
 * @author Dave Syer
 */
public class CustomCommandFactory implements CommandFactory {

	@Override
	public Collection<Command> getCommands() {
		return Collections.singleton(new CustomCommand());
	}

}
