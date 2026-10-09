/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.env;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.origin.OriginTrackedValue;
import org.springframework.boot.origin.TextResourceOrigin;
import org.springframework.boot.origin.TextResourceOrigin.Location;
import org.springframework.core.io.Resource;
import org.springframework.util.StreamUtils;

/**
 * Internal class used to load {@code .env} files while tracking the {@link Location} of
 * each value.
 * <p>
 * Each line is either blank, a comment (starting with {@code #}), or a variable in the
 * form {@code [export ]KEY=VALUE}. The value can be:
 * <ul>
 * <li>unquoted, in which case it is trimmed and ends at the end of the line or at a
 * {@code #} that follows whitespace,</li>
 * <li>enclosed in double quotes, in which case it can span multiple lines and supports
 * the {@code \n}, {@code \r}, {@code \t}, {@code \"} and {@code \\} escape sequences,
 * or</li>
 * <li>enclosed in single quotes, in which case it can span multiple lines and is taken
 * literally.</li>
 * </ul>
 * Only whitespace or a comment can follow a quoted value.
 *
 * @author Sharang Gupta
 */
final class OriginTrackedDotEnvLoader {

	private static final String EXPORT_KEYWORD = "export";

	private static final char BYTE_ORDER_MARK = '﻿';

	private final Resource resource;

	private OriginTrackedDotEnvLoader(Resource resource) {
		this.resource = resource;
	}

	/**
	 * Load the variables of the given {@code .env} resource.
	 * @param resource the resource to load
	 * @param encoding the encoding of the resource or {@code null} to use UTF-8
	 * @return the variables, in the order they are defined
	 * @throws IOException if the resource cannot be read
	 * @throws IllegalStateException if the content is not valid
	 */
	static Map<String, Object> load(Resource resource, @Nullable Charset encoding) throws IOException {
		Charset charset = (encoding != null) ? encoding : StandardCharsets.UTF_8;
		try (InputStream inputStream = resource.getInputStream()) {
			String content = StreamUtils.copyToString(inputStream, charset);
			return new OriginTrackedDotEnvLoader(resource).parse(content);
		}
	}

	private Map<String, Object> parse(String content) {
		Map<String, Object> variables = new LinkedHashMap<>();
		Scanner scanner = new Scanner(content);
		while (scanner.hasMore()) {
			parseLine(scanner, variables);
		}
		return variables;
	}

	private void parseLine(Scanner scanner, Map<String, Object> variables) {
		scanner.skipBlanks();
		if (scanner.atEndOfLine()) {
			scanner.skipLineSeparator();
			return;
		}
		if (scanner.peek() == '#') {
			scanner.skipToEndOfLine();
			return;
		}
		skipExportKeyword(scanner);
		String key = readKey(scanner);
		scanner.skipBlanks();
		scanner.next(); // the '='
		boolean blanksBeforeValue = scanner.skipBlanks();
		Location location = scanner.location();
		String value = readValue(scanner, blanksBeforeValue);
		variables.put(key, OriginTrackedValue.of(value, new TextResourceOrigin(this.resource, location)));
	}

	private void skipExportKeyword(Scanner scanner) {
		if (scanner.startsWith(EXPORT_KEYWORD) && scanner.isBlank(EXPORT_KEYWORD.length())) {
			scanner.skip(EXPORT_KEYWORD.length());
			scanner.skipBlanks();
		}
	}

	private String readKey(Scanner scanner) {
		StringBuilder key = new StringBuilder();
		while (!scanner.atEndOfLine() && scanner.peek() != '=') {
			key.append(scanner.next());
		}
		if (scanner.atEndOfLine()) {
			throw invalid(scanner, "expected 'KEY=VALUE'");
		}
		String trimmed = key.toString().trim();
		if (trimmed.isEmpty()) {
			throw invalid(scanner, "the variable name is missing");
		}
		if (trimmed.chars().anyMatch(Character::isWhitespace)) {
			throw invalid(scanner, "the variable name contains whitespace");
		}
		return trimmed;
	}

	private String readValue(Scanner scanner, boolean blanksBeforeValue) {
		if (!scanner.atEndOfLine() && (scanner.peek() == '"' || scanner.peek() == '\'')) {
			return readQuotedValue(scanner);
		}
		return readUnquotedValue(scanner, blanksBeforeValue);
	}

	private String readUnquotedValue(Scanner scanner, boolean blanksBeforeValue) {
		StringBuilder value = new StringBuilder();
		char previous = (blanksBeforeValue) ? ' ' : '=';
		while (!scanner.atEndOfLine()) {
			char current = scanner.peek();
			if (current == '#' && isBlank(previous)) {
				scanner.advanceToEndOfLine();
				break;
			}
			value.append(scanner.next());
			previous = current;
		}
		scanner.skipLineSeparator();
		return value.toString().stripTrailing();
	}

	private String readQuotedValue(Scanner scanner) {
		int startLine = scanner.line();
		char quote = scanner.next();
		StringBuilder value = new StringBuilder();
		while (true) {
			if (!scanner.hasMore()) {
				throw invalid(startLine, "unterminated quoted value");
			}
			char current = scanner.nextCharacterOfQuotedValue();
			if (current == quote) {
				break;
			}
			if (current == '\\' && quote == '"' && scanner.hasMore()) {
				value.append(unescape(scanner));
				continue;
			}
			value.append(current);
		}
		scanner.skipBlanks();
		if (!scanner.atEndOfLine() && scanner.peek() != '#') {
			throw invalid(scanner, "unexpected text after the closing quote");
		}
		scanner.skipToEndOfLine();
		return value.toString();
	}

	private String unescape(Scanner scanner) {
		char escaped = scanner.nextCharacterOfQuotedValue();
		return switch (escaped) {
			case 'n' -> "\n";
			case 'r' -> "\r";
			case 't' -> "\t";
			case '"' -> "\"";
			case '\\' -> "\\";
			default -> "\\" + escaped;
		};
	}

	private static boolean isBlank(char character) {
		return character == ' ' || character == '\t';
	}

	private IllegalStateException invalid(Scanner scanner, String problem) {
		return invalid(scanner.line(), problem);
	}

	private IllegalStateException invalid(int zeroBasedLine, String problem) {
		return new IllegalStateException("Invalid .env content in %s at line %d: %s"
			.formatted(this.resource.getDescription(), zeroBasedLine + 1, problem));
	}

	/**
	 * Cursor over the content that keeps track of the current line and column.
	 */
	private static final class Scanner {

		private final String content;

		private int position;

		private int line;

		private int lineStart;

		Scanner(String content) {
			this.content = content;
			this.position = (!content.isEmpty() && content.charAt(0) == BYTE_ORDER_MARK) ? 1 : 0;
			this.lineStart = this.position;
		}

		boolean hasMore() {
			return this.position < this.content.length();
		}

		int line() {
			return this.line;
		}

		Location location() {
			return new Location(this.line, this.position - this.lineStart);
		}

		char peek() {
			return this.content.charAt(this.position);
		}

		char next() {
			return this.content.charAt(this.position++);
		}

		boolean startsWith(String prefix) {
			return this.content.startsWith(prefix, this.position);
		}

		boolean isBlank(int offset) {
			int index = this.position + offset;
			return index < this.content.length() && OriginTrackedDotEnvLoader.isBlank(this.content.charAt(index));
		}

		void skip(int count) {
			this.position += count;
		}

		boolean skipBlanks() {
			int start = this.position;
			while (hasMore() && OriginTrackedDotEnvLoader.isBlank(peek())) {
				this.position++;
			}
			return this.position > start;
		}

		boolean atEndOfLine() {
			return !hasMore() || peek() == '\n' || peek() == '\r';
		}

		void advanceToEndOfLine() {
			while (!atEndOfLine()) {
				this.position++;
			}
		}

		void skipToEndOfLine() {
			advanceToEndOfLine();
			skipLineSeparator();
		}

		void skipLineSeparator() {
			if (!atEndOfLine() || !hasMore()) {
				return;
			}
			char separator = next();
			if (separator == '\r' && hasMore() && peek() == '\n') {
				this.position++;
			}
			this.line++;
			this.lineStart = this.position;
		}

		/**
		 * Return the next character of a quoted value, in which a line separator of any
		 * kind is a single {@code \n}.
		 * @return the next character
		 */
		char nextCharacterOfQuotedValue() {
			char current = peek();
			if (current == '\n' || current == '\r') {
				skipLineSeparator();
				return '\n';
			}
			return next();
		}

	}

}
