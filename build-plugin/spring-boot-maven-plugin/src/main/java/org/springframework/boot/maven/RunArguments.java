/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.maven;

import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedList;

import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.jspecify.annotations.Nullable;

/**
 * Parse and expose arguments specified in a single string.
 *
 * @author Stephane Nicoll
 */
class RunArguments {

	private static final String[] NO_ARGS = {};

	private final Deque<String> args = new LinkedList<>();

	RunArguments(@Nullable String arguments) {
		this(parseArgs(arguments));
	}

	RunArguments(@Nullable String[] args) {
		this((args != null) ? Arrays.asList(args) : null);
	}

	RunArguments(@Nullable Iterable<@Nullable String> args) {
		if (args != null) {
			for (String arg : args) {
				if (arg != null) {
					this.args.add(arg);
				}
			}
		}
	}

	Deque<String> getArgs() {
		return this.args;
	}

	String[] asArray() {
		return this.args.toArray(new String[0]);
	}

	static String[] parseArgs(@Nullable String arguments) {
		if (arguments == null || arguments.trim().isEmpty()) {
			return NO_ARGS;
		}
		try {
			arguments = arguments.replace('\n', ' ').replace('\t', ' ');
			return CommandLineUtils.translateCommandline(arguments);
		}
		catch (Exception ex) {
			throw new IllegalArgumentException("Failed to parse arguments [" + arguments + "]", ex);
		}
	}

}
