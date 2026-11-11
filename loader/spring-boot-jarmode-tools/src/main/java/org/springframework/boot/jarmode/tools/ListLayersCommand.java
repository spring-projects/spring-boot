/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jarmode.tools;

import java.io.PrintStream;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

/**
 * The {@code 'list-layers'} tools command.
 *
 * @author Moritz Halbritter
 */
class ListLayersCommand extends Command {

	private final Context context;

	ListLayersCommand(Context context) {
		super("list-layers", "List layers from the jar that can be extracted", Options.none(), Parameters.none());
		this.context = context;
	}

	@Override
	void run(PrintStream out, Map<Option, @Nullable String> options, List<String> parameters) {
		Layers layers = Layers.get(this.context);
		printLayers(out, layers);
	}

	void printLayers(PrintStream out, Layers layers) {
		layers.forEach(out::println);
	}

}
