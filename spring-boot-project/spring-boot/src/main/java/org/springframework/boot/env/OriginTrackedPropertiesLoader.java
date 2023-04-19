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

package org.springframework.boot.env;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;

import org.springframework.boot.origin.Origin;
import org.springframework.boot.origin.OriginTrackedValue;
import org.springframework.boot.origin.TextResourceOrigin;
import org.springframework.boot.origin.TextResourceOrigin.Location;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

/**
 * Class to load {@code .properties} files into a map of {@code String} -&gt;
 * {@link OriginTrackedValue}. Also supports expansion of {@code name[]=a,b,c} list style
 * values.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 * @author Thiago Hirata
 * @author Guirong Hu
 */
class OriginTrackedPropertiesLoader {

	private final Resource resource;

	/**
	 * Create a new {@link OriginTrackedPropertiesLoader} instance.
	 * @param resource the resource of the {@code .properties} data
	 */
	OriginTrackedPropertiesLoader(Resource resource) {
		Assert.notNull(resource, "Resource must not be null");
		this.resource = resource;
	}

	/**
	 * Load {@code .properties} data and return a list of documents.
	 * @return the loaded properties
	 * @throws IOException on read error
	 */
	List<Document> load() throws IOException {
		return load(true);
	}

	/**
	 * Load {@code .properties} data and return a map of {@code String} ->
	 * {@link OriginTrackedValue}.
	 * @param expandLists if list {@code name[]=a,b,c} shortcuts should be expanded
	 * @return the loaded properties
	 * @throws IOException on read error
	 */
	List<Document> load(boolean expandLists) throws IOException {
		List<Document> documents = new ArrayList<>();
		Document document = new Document();
		StringBuilder buffer = new StringBuilder();
		try (CharacterReader reader = new CharacterReader(this.resource)) {
			while (reader.read()) {
				if (reader.isCommentPrefixCharacter()) {
					char commentPrefixCharacter = reader.getCharacter();
					if (isNewDocument(reader)) {
						if (!document.isEmpty()) {
							documents.add(document);
						}
						document = new Document();
					}
					else {
						if (document.isEmpty() && !documents.isEmpty()) {
							document = documents.remove(documents.size() - 1);
						}
						reader.setLastLineCommentPrefixCharacter(commentPrefixCharacter);
						reader.skipComment();
					}
				}
				else {
					reader.setLastLineCommentPrefixCharacter(-1);
					loadKeyAndValue(expandLists, document, reader, buffer);
				}
			}

		}
		if (!document.isEmpty() && !documents.contains(document)) {
			documents.add(document);
		}
		return documents;
	}

	private void loadKeyAndValue(boolean expandLists, Document document, CharacterReader reader, StringBuilder buffer)
			throws IOException {
		String key = loadKey(buffer, reader).trim();
		if (expandLists && key.endsWith("[]")) {
			key = key.substring(0, key.length() - 2);
			int index = 0;
			do {
				OriginTrackedValue value = loadValue(buffer, reader, true);
				document.put(key + "[" + (index++) + "]", value);
				if (!reader.isEndOfLine()) {
					reader.read();
				}
			}
			while (!reader.isEndOfLine());
		}
		else {
			OriginTrackedValue value = loadValue(buffer, reader, false);
			document.put(key, value);
		}
	}

	private String loadKey(StringBuilder buffer, CharacterReader reader) throws IOException {
		buffer.setLength(0);
		boolean previousWhitespace = false;
		while (!reader.isEndOfLine()) {
			if (reader.isPropertyDelimiter()) {
				reader.read();
				return buffer.toString();
			}
			if (!reader.isWhiteSpace() && previousWhitespace) {
				return buffer.toString();
			}
			previousWhitespace = reader.isWhiteSpace();
			buffer.append(reader.getCharacter());
			reader.read();
		}
		return buffer.toString();
	}

	private OriginTrackedValue loadValue(StringBuilder buffer, CharacterReader reader, boolean splitLists)
			throws IOException {
		buffer.setLength(0);
		while (reader.isWhiteSpace() && !reader.isEndOfLine()) {
			reader.read();
		}
		Location location = reader.getLocation();
		while (!reader.isEndOfLine() && !(splitLists && reader.isListDelimiter())) {
			buffer.append(reader.getCharacter());
			reader.read();
		}
		Origin origin = new TextResourceOrigin(this.resource, location);
		return OriginTrackedValue.of(buffer.toString(), origin);
	}

	private boolean isNewDocument(CharacterReader reader) throws IOException {
		if (reader.isSameLastLineCommentPrefix()) {
			return false;
		}
		boolean result = reader.getLocation().getColumn() == 0;
		result = result && readAndExpect(reader, reader::isHyphenCharacter);
		result = result && readAndExpect(reader, reader::isHyphenCharacter);
		result = result && readAndExpect(reader, reader::isHyphenCharacter);
		if (!reader.isEndOfLine()) {
			reader.read();
			reader.skipWhitespace();
		}
		return result && reader.isEndOfLine();
	}

	private boolean readAndExpect(CharacterReader reader, BooleanSupplier check) throws IOException {
		reader.read();
		return check.getAsBoolean();
	}

	/**
	 * Reads characters from the source resource, taking care of skipping comments,
	 * handling multi-line values and tracking {@code '\'} escapes.
	 */
	private static class CharacterReader implements Closeable {

		private static final String[] ESCAPES = { "trnf", "\t\r\n\f" };

		private final LineNumberReader reader;

		private int columnNumber = -1;

		private boolean escaped;

		private int character;

		private int lastLineCommentPrefixCharacter;

		CharacterReader(Resource resource) throws IOException {
			this.reader = new LineNumberReader(
					new InputStreamReader(resource.getInputStream(), StandardCharsets.ISO_8859_1));
		}

		@Override
		public void close() throws IOException {
			this.reader.close();
		}

		boolean read() throws IOException {
			this.escaped = false;
			this.character = this.reader.read();
			this.columnNumber++;
			if (this.columnNumber == 0) {
				skipWhitespace();
			}
			if (this.character == '\\') {
				this.escaped = true;
				readEscaped();
			}
			else if (this.character == '\n') {
				this.columnNumber = -1;
			}
			return !isEndOfFile();
		}

		private void skipWhitespace() throws IOException {
			while (isWhiteSpace()) {
				this.character = this.reader.read();
				this.columnNumber++;
			}
		}

		private void setLastLineCommentPrefixCharacter(int lastLineCommentPrefixCharacter) {
			this.lastLineCommentPrefixCharacter = lastLineCommentPrefixCharacter;
		}

		private void skipComment() throws IOException {
			while (this.character != '\n' && this.character != -1) {
				this.character = this.reader.read();
			}
			this.columnNumber = -1;
		}

		private void readEscaped() throws IOException {
			this.character = this.reader.read();
			int escapeIndex = ESCAPES[0].indexOf(this.character);
			if (escapeIndex != -1) {
				this.character = ESCAPES[1].charAt(escapeIndex);
			}
			else if (this.character == '\n') {
				this.columnNumber = -1;
				read();
			}
			else if (this.character == 'u') {
				readUnicode();
			}
		}

		private void readUnicode() throws IOException {
			this.character = 0;
			for (int i = 0; i < 4; i++) {
				int digit = this.reader.read();
				if (digit >= '0' && digit <= '9') {
					this.character = (this.character << 4) + digit - '0';
				}
				else if (digit >= 'a' && digit <= 'f') {
					this.character = (this.character << 4) + digit - 'a' + 10;
				}
				else if (digit >= 'A' && digit <= 'F') {
					this.character = (this.character << 4) + digit - 'A' + 10;
				}
				else {
					throw new IllegalStateException("Malformed \\uxxxx encoding.");
				}
			}
		}

		boolean isWhiteSpace() {
			return !this.escaped && (this.character == ' ' || this.character == '\t' || this.character == '\f');
		}

		boolean isEndOfFile() {
			return this.character == -1;
		}

		boolean isEndOfLine() {
			return this.character == -1 || (!this.escaped && this.character == '\n');
		}

		boolean isListDelimiter() {
			return !this.escaped && this.character == ',';
		}

		boolean isPropertyDelimiter() {
			return !this.escaped && (this.character == '=' || this.character == ':');
		}

		char getCharacter() {
			return (char) this.character;
		}

		Location getLocation() {
			return new Location(this.reader.getLineNumber(), this.columnNumber);
		}

		boolean isSameLastLineCommentPrefix() {
			return this.lastLineCommentPrefixCharacter == this.character;
		}

		boolean isCommentPrefixCharacter() {
			return this.character == '#' || this.character == '!';
		}

		boolean isHyphenCharacter() {
			return this.character == '-';
		}

	}

	/**
	 * A single document within the properties file.
	 */
	static class Document {

		private final Map<String, OriginTrackedValue> values = new LinkedHashMap<>();

		void put(String key, OriginTrackedValue value) {
			if (!key.isEmpty()) {
				this.values.put(key, value);
			}
		}

		boolean isEmpty() {
			return this.values.isEmpty();
		}

		Map<String, OriginTrackedValue> asMap() {
			return this.values;
		}

	}

}
