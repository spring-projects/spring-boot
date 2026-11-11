/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.cli.command.shell;

import jline.Terminal;
import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.AnsiRenderer.Code;

/**
 * Simple utility class to build an ANSI string when supported by the {@link Terminal}.
 *
 * @author Phillip Webb
 */
class AnsiString {

	private final Terminal terminal;

	private final StringBuilder value = new StringBuilder();

	/**
	 * Create a new {@link AnsiString} for the given {@link Terminal}.
	 * @param terminal the terminal used to test if {@link Terminal#isAnsiSupported() ANSI
	 * is supported}.
	 */
	AnsiString(Terminal terminal) {
		this.terminal = terminal;
	}

	/**
	 * Append text with the given ANSI codes.
	 * @param text the text to append
	 * @param codes the ANSI codes
	 * @return this string
	 */
	AnsiString append(String text, Code... codes) {
		if (codes.length == 0 || !isAnsiSupported()) {
			this.value.append(text);
			return this;
		}
		Ansi ansi = Ansi.ansi();
		for (Code code : codes) {
			ansi = applyCode(ansi, code);
		}
		this.value.append(ansi.a(text).reset().toString());
		return this;
	}

	private Ansi applyCode(Ansi ansi, Code code) {
		if (code.isColor()) {
			if (code.isBackground()) {
				return ansi.bg(code.getColor());
			}
			return ansi.fg(code.getColor());
		}
		return ansi.a(code.getAttribute());
	}

	private boolean isAnsiSupported() {
		return this.terminal.isAnsiSupported();
	}

	@Override
	public String toString() {
		return this.value.toString();
	}

}
