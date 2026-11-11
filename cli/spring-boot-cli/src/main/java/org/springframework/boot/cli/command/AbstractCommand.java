/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.cli.command;

import java.util.Collection;
import java.util.Collections;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.cli.command.options.OptionHelp;

/**
 * Abstract {@link Command} implementation.
 *
 * @author Phillip Webb
 * @author Dave Syer
 * @since 1.0.0
 */
public abstract class AbstractCommand implements Command {

	private final String name;

	private final String description;

	/**
	 * Create a new {@link AbstractCommand} instance.
	 * @param name the name of the command
	 * @param description the command description
	 */
	protected AbstractCommand(String name, String description) {
		this.name = name;
		this.description = description;
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public String getDescription() {
		return this.description;
	}

	@Override
	public @Nullable String getUsageHelp() {
		return null;
	}

	@Override
	public @Nullable String getHelp() {
		return null;
	}

	@Override
	public Collection<OptionHelp> getOptionsHelp() {
		return Collections.emptyList();
	}

	@Override
	public @Nullable Collection<HelpExample> getExamples() {
		return null;
	}

}
