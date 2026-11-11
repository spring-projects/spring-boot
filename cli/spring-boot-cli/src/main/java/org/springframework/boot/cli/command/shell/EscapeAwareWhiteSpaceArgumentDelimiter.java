/*
 * Copyright 2012-present the original author or authors.
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
		int openingQuote = searchBackwards(buffer, closingQuote - 1, buffer.charAt(closingQuote));
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

	String[] parseArguments(String line) {
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
		string = string.replace("\\'", "'");
		return string;
	}

}
