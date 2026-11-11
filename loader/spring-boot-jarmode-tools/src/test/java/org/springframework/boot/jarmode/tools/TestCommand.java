/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jarmode.tools;

import java.io.PrintStream;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

/**
 * @author Moritz Halbritter
 */
class TestCommand extends Command {

	TestCommand() {
		super("test", "Description of test",
				Options.of(Option.of("option1", "value1", "Description of option1"),
						Option.of("option2", "value2", "Description of option2")),
				Parameters.of("parameter1", "parameter2"));
	}

	@Override
	protected void run(PrintStream out, Map<Option, @Nullable String> options, List<String> parameters) {

	}

}
