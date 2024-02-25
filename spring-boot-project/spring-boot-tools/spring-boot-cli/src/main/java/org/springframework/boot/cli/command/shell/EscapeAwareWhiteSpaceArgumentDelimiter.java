/*
 * Copyright 2012-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
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

	/**
     * Determines if the character at the specified position in the given buffer is escaped.
     * 
     * @param buffer the character sequence buffer
     * @param pos the position of the character in the buffer
     * @return true if the character is escaped, false otherwise
     */
    @Override
	public boolean isEscaped(CharSequence buffer, int pos) {
		return (isEscapeChar(buffer, pos - 1));
	}

	/**
     * Checks if the character at the specified position in the given buffer is an escape character.
     * 
     * @param buffer the character sequence buffer to check
     * @param pos the position of the character to check
     * @return {@code true} if the character at the specified position is an escape character, {@code false} otherwise
     */
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

	/**
     * Determines if the specified position in the given buffer is within a quoted section.
     * 
     * @param buffer the character sequence to search within
     * @param pos the position to check
     * @return true if the position is within a quoted section, false otherwise
     */
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

	/**
     * Searches backwards in the given buffer for the first occurrence of any of the specified characters.
     * 
     * @param buffer the character sequence to search in
     * @param pos the starting position for the search
     * @param chars the characters to search for
     * @return the index of the first occurrence of any of the specified characters, or -1 if not found
     */
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

	/**
     * Parses the given line and returns an array of arguments.
     * 
     * @param line the line to be parsed
     * @return an array of parsed arguments
     */
    String[] parseArguments(String line) {
		ArgumentList delimit = delimit(line, 0);
		return cleanArguments(delimit.getArguments());
	}

	/**
     * Cleans the given array of arguments by removing any leading or trailing white spaces.
     * 
     * @param arguments the array of arguments to be cleaned
     * @return the cleaned array of arguments
     */
    private String[] cleanArguments(String[] arguments) {
		String[] cleanArguments = new String[arguments.length];
		for (int i = 0; i < arguments.length; i++) {
			cleanArguments[i] = cleanArgument(arguments[i]);
		}
		return cleanArguments;
	}

	/**
     * Cleans the given argument by removing any surrounding quotes and replacing escape sequences.
     * 
     * @param argument the argument to be cleaned
     * @return the cleaned argument
     */
    private String cleanArgument(String argument) {
		for (char c : getQuoteChars()) {
			String quote = String.valueOf(c);
			if (argument.startsWith(quote) && argument.endsWith(quote)) {
				return replaceEscapes(argument.substring(1, argument.length() - 1));
			}
		}
		return replaceEscapes(argument);
	}

	/**
     * Replaces escape sequences in a given string with their corresponding characters.
     * 
     * @param string the string to be processed
     * @return the processed string with escape sequences replaced
     */
    private String replaceEscapes(String string) {
		string = string.replace("\\ ", " ");
		string = string.replace("\\\\", "\\");
		string = string.replace("\\t", "\t");
		string = string.replace("\\\"", "\"");
		string = string.replace("\\'", "'");
		return string;
	}

}
