/*
 * Copyright 2012-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
