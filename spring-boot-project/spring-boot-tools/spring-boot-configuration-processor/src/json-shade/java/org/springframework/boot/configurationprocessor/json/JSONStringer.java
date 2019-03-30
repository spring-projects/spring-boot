/*
 * Copyright (C) 2010 The Android Open Source Project
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

package org.springframework.boot.configurationprocessor.json;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// Note: this class was written without inspecting the non-free org.json source code.

/**
 * Implements {@link JSONObject#toString} and {@link JSONArray#toString}. Most application
 * developers should use those methods directly and disregard this API. For example:<pre>
 * JSONObject object = ...
 * String json = object.toString();</pre>
 * <p>
 * Stringers only encode well-formed JSON strings. In particular:
 * <ul>
 * <li>The stringer must have exactly one top-level array or object.
 * <li>Lexical scopes must be balanced: every call to {@link #array} must have a matching
 * call to {@link #endArray} and every call to {@link #object} must have a matching call
 * to {@link #endObject}.
 * <li>Arrays may not contain keys (property names).
 * <li>Objects must alternate keys (property names) and values.
 * <li>Values are inserted with either literal {@link #value(Object) value} calls, or by
 * nesting arrays or objects.
 * </ul>
 * Calls that would result in a malformed JSON string will fail with a
 * {@link JSONException}.
 * <p>
 * This class provides no facility for pretty-printing (ie. indenting) output. To encode
 * indented output, use {@link JSONObject#toString(int)} or
 * {@link JSONArray#toString(int)}.
 * <p>
 * Some implementations of the API support at most 20 levels of nesting. Attempts to
 * create more than 20 levels of nesting may fail with a {@link JSONException}.
 * <p>
 * Each stringer may be used to encode a single top level value. Instances of this class
 * are not thread safe. Although this class is nonfinal, it was not designed for
 * inheritance and should not be subclassed. In particular, self-use by overrideable
 * methods is not specified. See <i>Effective Java</i> Item 17, "Design and Document or
 * inheritance or else prohibit it" for further information.
 */
public class JSONStringer {

	/**
	 * The output data, containing at most one top-level array or object.
	 */
	final StringBuilder out = new StringBuilder();

	/**
	 * Lexical scoping elements within this stringer, necessary to insert the appropriate
	 * separator characters (ie. commas and colons) and to detect nesting errors.
	 */
	enum Scope {

		/**
		 * An array with no elements requires no separators or newlines before it is
		 * closed.
		 */
		EMPTY_ARRAY,

		/**
		 * An array with at least one value requires a comma and newline before the next
		 * element.
		 */
		NONEMPTY_ARRAY,

		/**
		 * An object with no keys or values requires no separators or newlines before it
		 * is closed.
		 */
		EMPTY_OBJECT,

		/**
		 * An object whose most recent element is a key. The next element must be a value.
		 */
		DANGLING_KEY,

		/**
		 * An object with at least one name/value pair requires a comma and newline before
		 * the next element.
		 */
		NONEMPTY_OBJECT,

		/**
		 * A special bracketless array needed by JSONStringer.join() and
		 * JSONObject.quote() only. Not used for JSON encoding.
		 */
		NULL

	}

	/**
	 * Unlike the original implementation, this stack isn't limited to 20 levels of
	 * nesting.
	 */
	private final List<Scope> stack = new ArrayList<>();

	/**
	 * A string containing a full set of spaces for a single level of indentation, or null
	 * for no pretty printing.
	 */
	private final String indent;

	public JSONStringer() {
		this.indent = null;
	}

	JSONStringer(int indentSpaces) {
		char[] indentChars = new char[indentSpaces];
		Arrays.fill(indentChars, ' ');
		this.indent = new String(indentChars);
	}

	/**
	 * Begins encoding a new array. Each call to this method must be paired with a call to
	 * {@link #endArray}.
	 * @return this stringer.
	 * @throws JSONException if processing of json failed
	 */
	public JSONStringer array() throws JSONException {
		return open(Scope.EMPTY_ARRAY, "[");
	}

	/**
	 * Ends encoding the current array.
	 * @return this stringer.
	 * @throws JSONException if processing of json failed
	 */
	public JSONStringer endArray() throws JSONException {
		return close(Scope.EMPTY_ARRAY, Scope.NONEMPTY_ARRAY, "]");
	}

	/**
	 * Begins encoding a new object. Each call to this method must be paired with a call
	 * to {@link #endObject}.
	 * @return this stringer.
	 * @throws JSONException if processing of json failed
	 */
	public JSONStringer object() throws JSONException {
		return open(Scope.EMPTY_OBJECT, "{");
	}

	/**
	 * Ends encoding the current object.
	 * @return this stringer.
	 * @throws JSONException if processing of json failed
	 */
	public JSONStringer endObject() throws JSONException {
		return close(Scope.EMPTY_OBJECT, Scope.NONEMPTY_OBJECT, "}");
	}

	/**
	 * Enters a new scope by appending any necessary whitespace and the given bracket.
	 * @param empty any necessary whitespace
	 * @param openBracket the open bracket
	 * @return this object
	 * @throws JSONException if processing of json failed
	 */
	JSONStringer open(Scope empty, String openBracket) throws JSONException {
		if (this.stack.isEmpty() && this.out.length() > 0) {
			throw new JSONException("Nesting problem: multiple top-level roots");
		}
		beforeValue();
		this.stack.add(empty);
		this.out.append(openBracket);
		return this;
	}

	/**
	 * Closes the current scope by appending any necessary whitespace and the given
	 * bracket.
	 * @param empty any necessary whitespace
	 * @param nonempty the current scope
	 * @param closeBracket the close bracket
	 * @return the JSON stringer
	 * @throws JSONException if processing of json failed
	 */
	JSONStringer close(Scope empty, Scope nonempty, String closeBracket)
			throws JSONException {
		Scope context = peek();
		if (context != nonempty && context != empty) {
			throw new JSONException("Nesting problem");
		}

		this.stack.remove(this.stack.size() - 1);
		if (context == nonempty) {
			newline();
		}
		this.out.append(closeBracket);
		return this;
	}

	/**
	 * Returns the value on the top of the stack.
	 * @return the scope
	 * @throws JSONException if processing of json failed
	 */
	private Scope peek() throws JSONException {
		if (this.stack.isEmpty()) {
			throw new JSONException("Nesting problem");
		}
		return this.stack.get(this.stack.size() - 1);
	}

	/**
	 * Replace the value on the top of the stack with the given value.
	 * @param topOfStack the scope at the top of the stack
	 */
	private void replaceTop(Scope topOfStack) {
		this.stack.set(this.stack.size() - 1, topOfStack);
	}

	/**
	 * Encodes {@code value}.
	 * @param value a {@link JSONObject}, {@link JSONArray}, String, Boolean, Integer,
	 * Long, Double or null. May not be {@link Double#isNaN() NaNs} or
	 * {@link Double#isInfinite() infinities}.
	 * @return this stringer.
	 * @throws JSONException if processing of json failed
	 */
	public JSONStringer value(Object value) throws JSONException {
		if (this.stack.isEmpty()) {
			throw new JSONException("Nesting problem");
		}

		if (value instanceof JSONArray) {
			((JSONArray) value).writeTo(this);
			return this;

		}
		else if (value instanceof JSONObject) {
			((JSONObject) value).writeTo(this);
			return this;
		}

		beforeValue();

		if (value == null || value instanceof Boolean || value == JSONObject.NULL) {
			this.out.append(value);

		}
		else if (value instanceof Number) {
			this.out.append(JSONObject.numberToString((Number) value));

		}
		else {
			string(value.toString());
		}

		return this;
	}

	/**
	 * Encodes {@code value} to this stringer.
	 * @param value the value to encode
	 * @return this stringer.
	 * @throws JSONException if processing of json failed
	 */
	public JSONStringer value(boolean value) throws JSONException {
		if (this.stack.isEmpty()) {
			throw new JSONException("Nesting problem");
		}
		beforeValue();
		this.out.append(value);
		return this;
	}

	/**
	 * Encodes {@code value} to this stringer.
	 * @param value a finite value. May not be {@link Double#isNaN() NaNs} or
	 * {@link Double#isInfinite() infinities}.
	 * @return this stringer.
	 * @throws JSONException if processing of json failed
	 */
	public JSONStringer value(double value) throws JSONException {
		if (this.stack.isEmpty()) {
			throw new JSONException("Nesting problem");
		}
		beforeValue();
		this.out.append(JSONObject.numberToString(value));
		return this;
	}

	/**
	 * Encodes {@code value} to this stringer.
	 * @param value the value to encode
	 * @return this stringer.
	 * @throws JSONException if processing of json failed
	 */
	public JSONStringer value(long value) throws JSONException {
		if (this.stack.isEmpty()) {
			throw new JSONException("Nesting problem");
		}
		beforeValue();
		this.out.append(value);
		return this;
	}

	private void string(String value) {
		this.out.append("\"");
		for (int i = 0, length = value.length(); i < length; i++) {
			char c = value.charAt(i);

			/*
			 * From RFC 4627, "All Unicode characters may be placed within the quotation
			 * marks except for the characters that must be escaped: quotation mark,
			 * reverse solidus, and the control characters (U+0000 through U+001F)."
			 */
			switch (c) {
			case '"':
			case '\\':
			case '/':
				this.out.append('\\').append(c);
				break;

			case '\t':
				this.out.append("\\t");
				break;

			case '\b':
				this.out.append("\\b");
				break;

			case '\n':
				this.out.append("\\n");
				break;

			case '\r':
				this.out.append("\\r");
				break;

			case '\f':
				this.out.append("\\f");
				break;

			default:
				if (c <= 0x1F) {
					this.out.append(String.format("\\u%04x", (int) c));
				}
				else {
					this.out.append(c);
				}
				break;
			}

		}
		this.out.append("\"");
	}

	private void newline() {
		if (this.indent == null) {
			return;
		}

		this.out.append("\n");
		for (int i = 0; i < this.stack.size(); i++) {
			this.out.append(this.indent);
		}
	}

	/**
	 * Encodes the key (property name) to this stringer.
	 * @param name the name of the forthcoming value. May not be null.
	 * @return this stringer.
	 * @throws JSONException if processing of json failed
	 */
	public JSONStringer key(String name) throws JSONException {
		if (name == null) {
			throw new JSONException("Names must be non-null");
		}
		beforeKey();
		string(name);
		return this;
	}

	/**
	 * Inserts any necessary separators and whitespace before a name. Also adjusts the
	 * stack to expect the key's value.
	 * @throws JSONException if processing of json failed
	 */
	private void beforeKey() throws JSONException {
		Scope context = peek();
		if (context == Scope.NONEMPTY_OBJECT) { // first in object
			this.out.append(',');
		}
		else if (context != Scope.EMPTY_OBJECT) { // not in an object!
			throw new JSONException("Nesting problem");
		}
		newline();
		replaceTop(Scope.DANGLING_KEY);
	}

	/**
	 * Inserts any necessary separators and whitespace before a literal value, inline
	 * array, or inline object. Also adjusts the stack to expect either a closing bracket
	 * or another element.
	 * @throws JSONException if processing of json failed
	 */
	private void beforeValue() throws JSONException {
		if (this.stack.isEmpty()) {
			return;
		}

		Scope context = peek();
		if (context == Scope.EMPTY_ARRAY) { // first in array
			replaceTop(Scope.NONEMPTY_ARRAY);
			newline();
		}
		else if (context == Scope.NONEMPTY_ARRAY) { // another in array
			this.out.append(',');
			newline();
		}
		else if (context == Scope.DANGLING_KEY) { // value for key
			this.out.append(this.indent == null ? ":" : ": ");
			replaceTop(Scope.NONEMPTY_OBJECT);
		}
		else if (context != Scope.NULL) {
			throw new JSONException("Nesting problem");
		}
	}

	/**
	 * Returns the encoded JSON string.
	 * <p>
	 * If invoked with unterminated arrays or unclosed objects, this method's return value
	 * is undefined.
	 * <p>
	 * <strong>Warning:</strong> although it contradicts the general contract of
	 * {@link Object#toString}, this method returns null if the stringer contains no data.
	 * @return the encoded JSON string.
	 */
	@Override
	public String toString() {
		return this.out.length() == 0 ? null : this.out.toString();
	}

}
