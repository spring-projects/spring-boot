/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jarmode.tools;

import java.util.List;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.loader.jarmode.JarMode;

/**
 * {@link JarMode} providing {@code "layertools"} support.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 * @since 2.3.0
 */
public class LayerToolsJarMode implements JarMode {

	static @Nullable Context contextOverride;

	@Override
	public boolean accepts(String mode) {
		return "layertools".equalsIgnoreCase(mode);
	}

	@Override
	public void run(String mode, String[] args) {
		try {
			Context context = (contextOverride != null) ? contextOverride : new Context();
			new Runner(System.out, context, getCommands(context)).run(args);
		}
		catch (Exception ex) {
			throw new IllegalStateException(ex);
		}
	}

	static List<Command> getCommands(Context context) {
		return List.of(new ListCommand(context), new ExtractLayersCommand(context));
	}

}
