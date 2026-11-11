/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jarmode.tools;

import java.io.PrintStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import org.springframework.util.StringUtils;

/**
 * The {@code 'extract'} tools command.
 *
 * @author Phillip Webb
 */
class ExtractLayersCommand extends Command {

	static final Option DESTINATION_OPTION = Option.of("destination", "string", "The destination to extract files to");

	private final ExtractCommand delegate;

	ExtractLayersCommand(Context context) {
		this(context, null);
	}

	ExtractLayersCommand(Context context, @Nullable Layers layers) {
		super("extract", "Extracts layers from the jar for image creation", Options.of(DESTINATION_OPTION),
				Parameters.of("[<layer>...]"));
		this.delegate = new ExtractCommand(context, layers);
	}

	@Override
	boolean isDeprecated() {
		return true;
	}

	@Override
	String getDeprecationMessage() {
		return "Use '-Djarmode=tools extract --layers --launcher' instead.";
	}

	@Override
	void run(PrintStream out, Map<Option, @Nullable String> options, List<String> parameters) {
		Map<Option, @Nullable String> rewrittenOptions = new HashMap<>();
		rewrittenOptions.put(ExtractCommand.DESTINATION_OPTION, options.getOrDefault(DESTINATION_OPTION, "."));
		rewrittenOptions.put(ExtractCommand.LAYERS_OPTION, StringUtils.collectionToCommaDelimitedString(parameters));
		rewrittenOptions.put(ExtractCommand.LAUNCHER_OPTION, null);
		rewrittenOptions.put(ExtractCommand.FORCE_OPTION, null);
		this.delegate.run(out, rewrittenOptions, Collections.emptyList());
	}

}
