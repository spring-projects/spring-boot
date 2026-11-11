/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jarmode.tools;

import java.io.PrintStream;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

/**
 * The {@code 'list'} tools command.
 *
 * Delegates the actual work to {@link ListLayersCommand}.
 *
 * @author Phillip Webb
 * @author Moritz Halbritter
 */
class ListCommand extends Command {

	private final ListLayersCommand delegate;

	ListCommand(Context context) {
		super("list", "List layers from the jar that can be extracted", Options.none(), Parameters.none());
		this.delegate = new ListLayersCommand(context);
	}

	@Override
	boolean isDeprecated() {
		return true;
	}

	@Override
	String getDeprecationMessage() {
		return "Use '-Djarmode=tools list-layers' instead.";
	}

	@Override
	void run(PrintStream out, Map<Option, @Nullable String> options, List<String> parameters) {
		this.delegate.run(out, options, parameters);
	}

	void printLayers(Layers layers, PrintStream out) {
		this.delegate.printLayers(out, layers);
	}

}
