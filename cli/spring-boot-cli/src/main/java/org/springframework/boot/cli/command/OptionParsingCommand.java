/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.cli.command;

import java.util.Collection;

import org.springframework.boot.cli.command.options.OptionHandler;
import org.springframework.boot.cli.command.options.OptionHelp;
import org.springframework.boot.cli.command.status.ExitStatus;

/**
 * Base class for a {@link Command} that parse options using an {@link OptionHandler}.
 *
 * @author Phillip Webb
 * @author Dave Syer
 * @since 1.0.0
 * @see OptionHandler
 */
public abstract class OptionParsingCommand extends AbstractCommand {

	private final OptionHandler handler;

	protected OptionParsingCommand(String name, String description, OptionHandler handler) {
		super(name, description);
		this.handler = handler;
	}

	@Override
	public String getHelp() {
		return this.handler.getHelp();
	}

	@Override
	public Collection<OptionHelp> getOptionsHelp() {
		return this.handler.getOptionsHelp();
	}

	@Override
	public final ExitStatus run(String... args) throws Exception {
		return this.handler.run(args);
	}

	protected OptionHandler getHandler() {
		return this.handler;
	}

}
