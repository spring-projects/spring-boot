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

import jline.console.completer.ArgumentCompleter.ArgumentList;
import jline.console.completer.ArgumentCompleter.WhitespaceArgumentDelimiter;

/**
 * Escape aware variant of {@link WhitespaceArgumentDelimiter}.
 *
 * @author Phillip Webb
 */
class EscapeAwareWhiteSpaceArgumentDelimiter extends WhitespaceArgumentDelimiter {

	@Override
	public boolean isEscaped(CharSequence buffer, int pos) {
		return (isEscapeChar(buffer, pos - 1));
	}

	private boolean isEscapeChar(CharSequence buffer, int pos) {
		if (pos >= 0) {
			for (char c : getEscapeChars()) {
				if (buffer.charAt(pos) == c) {
					return !isEscapeChar(buffer, pos - 1);
				}
			}
		}
		return false;
	}

	@Override
	public boolean isQuoted(CharSequence buffer, int pos) {
		int closingQuote = searchBackwards(buffer, pos - 1, getQuoteChars());
		if (closingQuote == -1) {
			return false;
		}
		int openingQuote = searchBackwards(buffer, closingQuote - 1,
				buffer.charAt(closingQuote));
		if (openingQuote == -1) {
			return true;
		}
		return isQuoted(buffer, openingQuote - 1);
	}

	private int searchBackwards(CharSequence buffer, int pos, char... chars) {
		while (pos >= 0) {
			for (char c : chars) {
				if (buffer.charAt(pos) == c && !isEscaped(buffer, pos)) {
					return pos;
				}
			}
			pos--;
		}
		return -1;
	}

	public String[] parseArguments(String line) {
		ArgumentList delimit = delimit(line, 0);
		return cleanArguments(delimit.getArguments());
	}

	private String[] cleanArguments(String[] arguments) {
		String[] cleanArguments = new String[arguments.length];
		for (int i = 0; i < arguments.length; i++) {
			cleanArguments[i] = cleanArgument(arguments[i]);
		}
		return cleanArguments;
	}

	private String cleanArgument(String argument) {
		for (char c : getQuoteChars()) {
			String quote = String.valueOf(c);
			if (argument.startsWith(quote) && argument.endsWith(quote)) {
				return replaceEscapes(argument.substring(1, argument.length() - 1));
			}
		}
		return replaceEscapes(argument);
	}

	private String replaceEscapes(String string) {
		string = string.replace("\\ ", " ");
		string = string.replace("\\\\", "\\");
		string = string.replace("\\t", "\t");
		string = string.replace("\\\"", "\"");
		string = string.replace("\\\'", "\'");
		return string;
	}

}
