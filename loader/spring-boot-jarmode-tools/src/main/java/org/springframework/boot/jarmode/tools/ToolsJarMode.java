/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jarmode.tools;

import java.io.PrintStream;
import java.util.List;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.loader.jarmode.JarMode;
import org.springframework.boot.loader.jarmode.JarModeErrorException;

/**
 * {@link JarMode} providing {@code "tools"} support.
 *
 * @author Moritz Halbritter
 * @author Hyeongjun Cho
 * @since 3.3.0
 */
public class ToolsJarMode implements JarMode {

	private final Context context;

	private final PrintStream out;

	public ToolsJarMode() {
		this(null, null);
	}

	public ToolsJarMode(@Nullable Context context, @Nullable PrintStream out) {
		this.context = (context != null) ? context : new Context();
		this.out = (out != null) ? out : System.out;
	}

	@Override
	public boolean accepts(String mode) {
		return "tools".equalsIgnoreCase(mode);
	}

	@Override
	public void run(String mode, String[] args) {
		try {
			new Runner(this.out, this.context, getCommands(this.context)).run(args);
		}
		catch (JarModeErrorException ex) {
			throw ex;
		}
		catch (Exception ex) {
			throw new IllegalStateException(ex);
		}
	}

	static List<Command> getCommands(Context context) {
		return List.of(new ExtractCommand(context), new ListLayersCommand(context), new SbomCommand(context));
	}

}
