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

	/**
	 * Loads a key-value pair from the input, and adds it to the document.
	 * @param expandLists a boolean indicating whether to expand lists
	 * @param document the document to add the key-value pair to
	 * @param reader the character reader for reading the input
	 * @param buffer the string builder for storing the key
	 * @throws IOException if an I/O error occurs while reading the input
	 */
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

	/**
	 * Loads a key from the given StringBuilder buffer and CharacterReader.
	 * @param buffer The StringBuilder buffer to load the key into.
	 * @param reader The CharacterReader to read characters from.
	 * @return The loaded key as a String.
	 * @throws IOException if an I/O error occurs while reading characters.
	 */
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

	/**
	 * Loads a value from the given StringBuilder and CharacterReader, while tracking its
	 * origin.
	 * @param buffer the StringBuilder to load the value into
	 * @param reader the CharacterReader to read the value from
	 * @param splitLists true if lists should be split, false otherwise
	 * @return the loaded value with its origin tracked
	 * @throws IOException if an I/O error occurs while reading the value
	 */
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

	/**
	 * Checks if the document is new.
	 * @param reader the CharacterReader object used to read the document
	 * @return true if the document is new, false otherwise
	 * @throws IOException if an I/O error occurs while reading the document
	 */
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

	/**
	 * Reads a character from the given reader and expects a boolean value based on the
	 * provided check.
	 * @param reader The CharacterReader to read from.
	 * @param check The BooleanSupplier to check the expected boolean value.
	 * @return true if the expected boolean value is true, false otherwise.
	 * @throws IOException if an I/O error occurs while reading from the reader.
	 */
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

		/**
		 * Constructs a new CharacterReader object with the given resource.
		 * @param resource the resource to read characters from
		 * @throws IOException if an I/O error occurs while reading the resource
		 */
		CharacterReader(Resource resource) throws IOException {
			this.reader = new LineNumberReader(
					new InputStreamReader(resource.getInputStream(), StandardCharsets.ISO_8859_1));
		}

		/**
		 * Closes the CharacterReader by closing the underlying reader.
		 * @throws IOException if an I/O error occurs while closing the reader
		 */
		@Override
		public void close() throws IOException {
			this.reader.close();
		}

		/**
		 * Reads a character from the input stream and updates the state of the
		 * CharacterReader object.
		 * @return true if the end of the file has not been reached, false otherwise
		 * @throws IOException if an I/O error occurs while reading the character
		 */
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

		/**
		 * Skips any whitespace characters in the input stream.
		 * @throws IOException if an I/O error occurs while reading the input stream
		 */
		private void skipWhitespace() throws IOException {
			while (isWhiteSpace()) {
				this.character = this.reader.read();
				this.columnNumber++;
			}
		}

		/**
		 * Sets the last line comment prefix character.
		 * @param lastLineCommentPrefixCharacter the character to set as the last line
		 * comment prefix
		 */
		private void setLastLineCommentPrefixCharacter(int lastLineCommentPrefixCharacter) {
			this.lastLineCommentPrefixCharacter = lastLineCommentPrefixCharacter;
		}

		/**
		 * Skips the current line comment in the input stream.
		 * @throws IOException if an I/O error occurs while reading the input stream
		 */
		private void skipComment() throws IOException {
			while (this.character != '\n' && this.character != -1) {
				this.character = this.reader.read();
			}
			this.columnNumber = -1;
		}

		/**
		 * Reads the next character from the input stream, taking into account any escaped
		 * characters.
		 * @throws IOException if an I/O error occurs while reading the character
		 */
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

		/**
		 * Reads a Unicode character from the input stream.
		 * @throws IOException if an I/O error occurs while reading the character
		 * @throws IllegalStateException if the \\uxxxx encoding is malformed
		 */
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

		/**
		 * Checks if the current character is a white space character.
		 * @return true if the current character is a white space character, false
		 * otherwise.
		 */
		boolean isWhiteSpace() {
			return !this.escaped && (this.character == ' ' || this.character == '\t' || this.character == '\f');
		}

		/**
		 * Checks if the current character is the end of the file.
		 * @return true if the current character is the end of the file, false otherwise.
		 */
		boolean isEndOfFile() {
			return this.character == -1;
		}

		/**
		 * Checks if the current character is the end of a line.
		 * @return true if the current character is the end of a line, false otherwise.
		 */
		boolean isEndOfLine() {
			return this.character == -1 || (!this.escaped && this.character == '\n');
		}

		/**
		 * Checks if the current character is a list delimiter.
		 * @return true if the current character is a list delimiter, false otherwise
		 */
		boolean isListDelimiter() {
			return !this.escaped && this.character == ',';
		}

		/**
		 * Checks if the current character is a property delimiter.
		 * @return true if the current character is '=' or ':', false otherwise
		 */
		boolean isPropertyDelimiter() {
			return !this.escaped && (this.character == '=' || this.character == ':');
		}

		/**
		 * Returns the character value of the CharacterReader object.
		 * @return the character value of the CharacterReader object.
		 */
		char getCharacter() {
			return (char) this.character;
		}

		/**
		 * Returns the current location of the character reader.
		 * @return the current location as a Location object
		 */
		Location getLocation() {
			return new Location(this.reader.getLineNumber(), this.columnNumber);
		}

		/**
		 * Checks if the last line comment prefix character is the same as the specified
		 * character.
		 * @return true if the last line comment prefix character is the same as the
		 * specified character, false otherwise.
		 */
		boolean isSameLastLineCommentPrefix() {
			return this.lastLineCommentPrefixCharacter == this.character;
		}

		/**
		 * Checks if the current character is a comment prefix character. A comment prefix
		 * character can be either '#' or '!'.
		 * @return true if the current character is a comment prefix character, false
		 * otherwise.
		 */
		boolean isCommentPrefixCharacter() {
			return this.character == '#' || this.character == '!';
		}

		/**
		 * Checks if the character is a hyphen character.
		 * @return true if the character is a hyphen character, false otherwise.
		 */
		boolean isHyphenCharacter() {
			return this.character == '-';
		}

	}

	/**
	 * A single document within the properties file.
	 */
	static class Document {

		private final Map<String, OriginTrackedValue> values = new LinkedHashMap<>();

		/**
		 * Puts a key-value pair into the document.
		 * @param key the key to be associated with the value
		 * @param value the value to be stored
		 */
		void put(String key, OriginTrackedValue value) {
			if (!key.isEmpty()) {
				this.values.put(key, value);
			}
		}

		/**
		 * Returns true if the document is empty, false otherwise.
		 * @return true if the document is empty, false otherwise
		 */
		boolean isEmpty() {
			return this.values.isEmpty();
		}

		/**
		 * Returns a map representation of the values in the Document.
		 * @return a map containing the values in the Document
		 */
		Map<String, OriginTrackedValue> asMap() {
			return this.values;
		}

	}

}
