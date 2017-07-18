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

package org.springframework.boot.context.properties.source;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * A configuration property name composed of elements separated by dots. User created
 * names may contain the characters "{@code a-z}" "{@code 0-9}") and "{@code -}", they
 * must be lower-case and must start with a letter. The "{@code -}" is used purely for
 * formatting, i.e. "{@code foo-bar}" and "{@code foobar}" are considered equivalent.
 * <p>
 * The "{@code [}" and "{@code ]}" characters may be used to indicate an associative
 * index(i.e. a {@link Map} key or a {@link Collection} index. Indexes names are not
 * restricted and are considered case-sensitive.
 * <p>
 * Here are some typical examples:
 * <ul>
 * <li>{@code spring.main.banner-mode}</li>
 * <li>{@code server.hosts[0].name}</li>
 * <li>{@code log[org.springboot].level}</li>
 * </ul>
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @since 2.0.0
 * @see #of(CharSequence)
 * @see ConfigurationPropertySource
 */
public final class ConfigurationPropertyName
		implements Comparable<ConfigurationPropertyName> {

	private static final String EMPTY_STRING = "";

	/**
	 * An empty {@link ConfigurationPropertyName}.
	 */
	public static final ConfigurationPropertyName EMPTY = new ConfigurationPropertyName(
			new String[0]);

	private final CharSequence[] elements;

	private final CharSequence[] uniformElements;

	private int[] elementHashCodes;

	private String string;

	private ConfigurationPropertyName(CharSequence[] elements) {
		this(elements, new CharSequence[elements.length]);
	}

	private ConfigurationPropertyName(CharSequence[] elements,
			CharSequence[] uniformElements) {
		this.elements = elements;
		this.uniformElements = uniformElements;
	}

	/**
	 * Returns {@code true} if this {@link ConfigurationPropertyName} is empty.
	 * @return {@code true} if the name is empty
	 */
	public boolean isEmpty() {
		return this.elements.length == 0;
	}

	/**
	 * Return if the last element in the name is indexed.
	 * @return {@code true} if the last element is indexed
	 */
	public boolean isLastElementIndexed() {
		int size = getNumberOfElements();
		return (size > 0 && isIndexed(this.elements[size - 1]));
	}

	/**
	 * Return if the element in the name is indexed.
	 * @param elementIndex the index of the element
	 * @return {@code true} if the element is indexed
	 */
	boolean isIndexed(int elementIndex) {
		return isIndexed(this.elements[elementIndex]);
	}

	/**
	 * Return if the element in the name is indexed and numeric.
	 * @param elementIndex the index of the element
	 * @return {@code true} if the element is indexed and numeric
	 */
	public boolean isNumericIndex(int elementIndex) {
		return isIndexed(elementIndex)
				&& isNumeric(getElement(elementIndex, Form.ORIGINAL));
	}

	private boolean isNumeric(CharSequence element) {
		for (int i = 0; i < element.length(); i++) {
			if (!Character.isDigit(element.charAt(i))) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Return the last element in the name in the given form.
	 * @param form the form to return
	 * @return the last element
	 */
	public String getLastElement(Form form) {
		int size = getNumberOfElements();
		return (size == 0 ? EMPTY_STRING : getElement(size - 1, form));
	}

	/**
	 * Return an element in the name in the given form.
	 * @param elementIndex the element index
	 * @param form the form to return
	 * @return the last element
	 */
	public String getElement(int elementIndex, Form form) {
		if (form == Form.ORIGINAL) {
			CharSequence result = this.elements[elementIndex];
			if (isIndexed(result)) {
				result = result.subSequence(1, result.length() - 1);
			}
			return result.toString();
		}
		CharSequence result = this.uniformElements[elementIndex];
		if (result == null) {
			result = this.elements[elementIndex];
			if (isIndexed(result)) {
				result = result.subSequence(1, result.length() - 1);
			}
			else {
				result = cleanupCharSequence(result, (c, i) -> c == '-' || c == '_',
						CharProcessor.LOWERCASE);
			}
			this.uniformElements[elementIndex] = result;
		}
		return result.toString();
	}

	/**
	 * Return the total number of elements in the name.
	 * @return the number of elements
	 */
	public int getNumberOfElements() {
		return this.elements.length;
	}

	/**
	 * Create a new {@link ConfigurationPropertyName} by appending the given element
	 * value.
	 * @param elementValue the single element value to append
	 * @return a new {@link ConfigurationPropertyName}
	 * @throws InvalidConfigurationPropertyNameException if elementValue is not valid
	 */
	public ConfigurationPropertyName append(String elementValue) {
		if (elementValue == null) {
			return this;
		}
		process(elementValue, '.', (value, start, end, indexed) -> Assert.isTrue(
				start == 0,
				() -> "Element value '" + elementValue + "' must be a single item"));
		if (!isIndexed(elementValue)) {
			InvalidConfigurationPropertyNameException.throwIfHasInvalidChars(elementValue,
					ElementValidator.getInvalidChars(elementValue));
		}
		int length = this.elements.length;
		CharSequence[] elements = new CharSequence[length + 1];
		System.arraycopy(this.elements, 0, elements, 0, length);
		elements[length] = elementValue;
		CharSequence[] uniformElements = new CharSequence[length + 1];
		System.arraycopy(this.uniformElements, 0, uniformElements, 0, length);
		return new ConfigurationPropertyName(elements, uniformElements);
	}

	/**
	 * Return a new {@link ConfigurationPropertyName} by chopping this name to the given
	 * {@code size}. For example, {@code chop(1)} on the name {@code foo.bar} will return
	 * {@code foo}.
	 * @param size the size to chop
	 * @return the chopped name
	 */
	public ConfigurationPropertyName chop(int size) {
		if (size >= getNumberOfElements()) {
			return this;
		}
		CharSequence[] elements = new CharSequence[size];
		System.arraycopy(this.elements, 0, elements, 0, size);
		CharSequence[] uniformElements = new CharSequence[size];
		System.arraycopy(this.uniformElements, 0, uniformElements, 0, size);
		return new ConfigurationPropertyName(elements, uniformElements);
	}

	/**
	 * Returns {@code true} if this element is an immediate parent of the specified name.
	 * @param name the name to check
	 * @return {@code true} if this name is an ancestor
	 */
	public boolean isParentOf(ConfigurationPropertyName name) {
		Assert.notNull(name, "Name must not be null");
		if (this.getNumberOfElements() != name.getNumberOfElements() - 1) {
			return false;
		}
		return isAncestorOf(name);
	}

	/**
	 * Returns {@code true} if this element is an ancestor (immediate or nested parent) of
	 * the specified name.
	 * @param name the name to check
	 * @return {@code true} if this name is an ancestor
	 */
	public boolean isAncestorOf(ConfigurationPropertyName name) {
		Assert.notNull(name, "Name must not be null");
		if (this.getNumberOfElements() >= name.getNumberOfElements()) {
			return false;
		}
		for (int i = 0; i < this.elements.length; i++) {
			if (!elementEquals(this.elements[i], name.elements[i])) {
				return false;
			}
		}
		return true;
	}

	@Override
	public int compareTo(ConfigurationPropertyName other) {
		return compare(this, other);
	}

	private int compare(ConfigurationPropertyName n1, ConfigurationPropertyName n2) {
		int l1 = n1.getNumberOfElements();
		int l2 = n2.getNumberOfElements();
		int i1 = 0;
		int i2 = 0;
		while (i1 < l1 || i2 < l2) {
			boolean indexed1 = (i1 < l1 ? n1.isIndexed(i2) : false);
			boolean indexed2 = (i2 < l2 ? n2.isIndexed(i2) : false);
			String e1 = (i1 < l1 ? n1.getElement(i1++, Form.UNIFORM) : null);
			String e2 = (i2 < l2 ? n2.getElement(i2++, Form.UNIFORM) : null);
			int result = compare(e1, indexed1, e2, indexed2);
			if (result != 0) {
				return result;
			}
		}
		return 0;
	}

	private int compare(String e1, boolean indexed1, String e2, boolean indexed2) {
		if (e1 == null) {
			return -1;
		}
		if (e2 == null) {
			return 1;
		}
		int result = Boolean.compare(indexed2, indexed1);
		if (result != 0) {
			return result;
		}
		if (indexed1 && indexed2) {
			try {
				long v1 = Long.parseLong(e1.toString());
				long v2 = Long.parseLong(e2.toString());
				return Long.compare(v1, v2);
			}
			catch (NumberFormatException ex) {
				// Fallback to string comparison
			}
		}
		return e1.compareTo(e2);
	}

	@Override
	public String toString() {
		if (this.string == null) {
			this.string = toString(this.elements);
		}
		return this.string;
	}

	private String toString(CharSequence[] elements) {
		StringBuilder result = new StringBuilder();
		for (CharSequence element : elements) {
			boolean indexed = isIndexed(element);
			if (result.length() > 0 && !indexed) {
				result.append(".");
			}
			if (indexed) {
				result.append(element);
			}
			else {
				for (int i = 0; i < element.length(); i++) {
					char ch = Character.toLowerCase(element.charAt(i));
					result.append(ch == '_' ? "" : ch);
				}
			}
		}
		return result.toString();
	}

	@Override
	public int hashCode() {
		if (this.elementHashCodes == null) {
			this.elementHashCodes = getElementHashCodes();
		}
		return ObjectUtils.nullSafeHashCode(this.elementHashCodes);
	}

	private int[] getElementHashCodes() {
		int[] hashCodes = new int[this.elements.length];
		for (int i = 0; i < this.elements.length; i++) {
			hashCodes[i] = getElementHashCode(this.elements[i]);
		}
		return hashCodes;
	}

	private int getElementHashCode(CharSequence element) {
		int hash = 0;
		boolean indexed = isIndexed(element);
		int offset = (indexed ? 1 : 0);
		for (int i = 0 + offset; i < element.length() - offset; i++) {
			char ch = (indexed ? element.charAt(i)
					: Character.toLowerCase(element.charAt(i)));
			hash = (ch == '-' || ch == '_' ? hash : 31 * hash + Character.hashCode(ch));
		}
		return hash;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (obj == null || !obj.getClass().equals(getClass())) {
			return false;
		}
		ConfigurationPropertyName other = (ConfigurationPropertyName) obj;
		if (getNumberOfElements() != other.getNumberOfElements()) {
			return false;
		}
		for (int i = 0; i < this.elements.length; i++) {
			if (!elementEquals(this.elements[i], other.elements[i])) {
				return false;
			}
		}
		return true;
	}

	private boolean elementEquals(CharSequence e1, CharSequence e2) {
		int l1 = e1.length();
		int l2 = e2.length();
		boolean indexed1 = isIndexed(e1);
		int offset1 = (indexed1 ? 1 : 0);
		boolean indexed2 = isIndexed(e2);
		int offset2 = (indexed2 ? 1 : 0);
		int i1 = offset1;
		int i2 = offset2;
		while (i1 < l1 - offset1) {
			if (i2 >= l2 - offset2) {
				return false;
			}
			char ch1 = (indexed1 ? e1.charAt(i1) : Character.toLowerCase(e1.charAt(i1)));
			char ch2 = (indexed2 ? e2.charAt(i2) : Character.toLowerCase(e2.charAt(i2)));
			if (ch1 == '-' || ch1 == '_') {
				i1++;
			}
			else if (ch2 == '-' || ch2 == '_') {
				i2++;
			}
			else if (ch1 != ch2) {
				return false;
			}
			else {
				i1++;
				i2++;
			}
		}
		while (i2 < l2 - offset2) {
			char ch = e2.charAt(i2++);
			if (ch != '-' && ch != '_') {
				return false;
			}
		}
		return true;
	}

	private static boolean isIndexed(CharSequence element) {
		int length = element.length();
		return length > 2 && element.charAt(0) == '['
				&& element.charAt(length - 1) == ']';
	}

	/**
	 * Returns if the given name is valid. If this method returns {@code true} then the
	 * name may be used with {@link #of(CharSequence)} without throwing an exception.
	 * @param name the name to test
	 * @return {@code true} if the name is valid
	 */
	public static boolean isValid(CharSequence name) {
		if (name == null) {
			return false;
		}
		if (name.equals(EMPTY_STRING)) {
			return true;
		}
		if (name.charAt(0) == '.' || name.charAt(name.length() - 1) == '.') {
			return false;
		}
		ElementValidator validator = new ElementValidator();
		process(name, '.', validator);
		return validator.isValid();
	}

	/**
	 * Return a {@link ConfigurationPropertyName} for the specified string.
	 * @param name the source name
	 * @return a {@link ConfigurationPropertyName} instance
	 * @throws InvalidConfigurationPropertyNameException if the name is not valid
	 */
	public static ConfigurationPropertyName of(CharSequence name) {
		Assert.notNull(name, "Name must not be null");
		if (name.length() >= 1
				&& (name.charAt(0) == '.' || name.charAt(name.length() - 1) == '.')) {
			throw new InvalidConfigurationPropertyNameException(name,
					Collections.singletonList('.'));
		}
		if (name.length() == 0) {
			return EMPTY;
		}
		List<CharSequence> elements = new ArrayList<>(10);
		process(name, '.', (elementValue, start, end, indexed) -> {
			if (elementValue.length() > 0) {
				if (!indexed) {
					InvalidConfigurationPropertyNameException.throwIfHasInvalidChars(name,
							ElementValidator.getInvalidChars(elementValue));
				}
				elements.add(elementValue);
			}
		});
		return new ConfigurationPropertyName(
				elements.toArray(new CharSequence[elements.size()]));
	}

	/**
	 * Create a {@link ConfigurationPropertyName} by adapting the given source. See
	 * {@link #adapt(CharSequence, char, Function)} for details.
	 * @param name the name to parse
	 * @param separator the separator used to split the name
	 * @return a {@link ConfigurationPropertyName}
	 */
	static ConfigurationPropertyName adapt(CharSequence name, char separator) {
		return adapt(name, separator, Function.identity());
	}

	/**
	 * Create a {@link ConfigurationPropertyName} by adapting the given source. The name
	 * is split into elements around the given {@code separator}. This method is more
	 * lenient than {@link #of} in that it allows mixed case names and '{@code _}'
	 * characters. Other invalid characters are stripped out during parsing.
	 * <p>
	 * The {@code elementValueProcessor} function may be used if additional processing is
	 * required on the extracted element values.
	 * @param name the name to parse
	 * @param separator the separator used to split the name
	 * @param elementValueProcessor a function to process element values
	 * @return a {@link ConfigurationPropertyName}
	 */
	static ConfigurationPropertyName adapt(CharSequence name, char separator,
			Function<CharSequence, CharSequence> elementValueProcessor) {
		Assert.notNull(name, "Name must not be null");
		Assert.notNull(elementValueProcessor, "ElementValueProcessor must not be null");
		if (name.length() == 0) {
			return EMPTY;
		}
		List<CharSequence> elements = new ArrayList<>(10);
		process(name, separator, (elementValue, start, end, indexed) -> {
			elementValue = elementValueProcessor.apply(elementValue);
			if (!isIndexed(elementValue)) {
				elementValue = cleanupCharSequence(elementValue,
						(ch, index) -> ch != '_' && !ElementValidator
								.isValidChar(Character.toLowerCase(ch), index),
						CharProcessor.NONE);
			}
			if (elementValue.length() > 0) {
				elements.add(elementValue);
			}
		});
		return new ConfigurationPropertyName(
				elements.toArray(new CharSequence[elements.size()]));
	}

	private static void process(CharSequence name, char separator,
			ElementProcessor processor) {
		int start = 0;
		boolean indexed = false;
		int length = name.length();
		int openBracketCount = 0;
		for (int i = 0; i < length; i++) {
			char ch = name.charAt(i);
			if (ch == ']') {
				openBracketCount--;
				if (openBracketCount == 0) {
					processElement(processor, name, start, i + 1, indexed);
					start = i + 1;
					indexed = false;
				}
			}
			else if (ch == '[') {
				openBracketCount++;
				if (!indexed) {
					processElement(processor, name, start, i, indexed);
					start = i;
					indexed = true;
				}
			}
			else if (!indexed && ch == separator) {
				processElement(processor, name, start, i, indexed);
				start = i + 1;
			}
		}
		processElement(processor, name, start, length, false);
	}

	private static void processElement(ElementProcessor processor, CharSequence name,
			int start, int end, boolean indexed) {
		if ((end - start) >= 1) {
			processor.process(name.subSequence(start, end), start, end, indexed);
		}
	}

	private static CharSequence cleanupCharSequence(CharSequence name, CharFilter filter,
			CharProcessor processor) {
		for (int i = 0; i < name.length(); i++) {
			char ch = name.charAt(i);
			char processed = processor.process(ch, i);
			if (filter.isExcluded(processed, i) || processed != ch) {
				// We save memory by only creating the new result if necessary
				StringBuilder result = new StringBuilder(name.length());
				result.append(name.subSequence(0, i));
				for (int j = i; j < name.length(); j++) {
					processed = processor.process(name.charAt(j), j);
					if (!filter.isExcluded(processed, j)) {
						result.append(processed);
					}
				}
				return result;
			}
		}
		return name;
	}

	/**
	 * The various forms that a non-indexed element value can take.
	 */
	public enum Form {

		/**
		 * The original form as specified when the name was created or parsed. For
		 * example:
		 * <ul>
		 * <li>"{@code foo-bar}" = "{@code foo-bar}"</li>
		 * <li>"{@code fooBar}" = "{@code fooBar}"</li>
		 * <li>"{@code foo_bar}" = "{@code foo_bar}"</li>
		 * <li>"{@code [Foo.bar]}" = "{@code Foo.bar}"</li>
		 * </ul>
		 */
		ORIGINAL,

		/**
		 * The uniform configuration form (used for equals/hashCode; lower-case with only
		 * alphanumeric characters).
		 * <ul>
		 * <li>"{@code foo-bar}" = "{@code foobar}"</li>
		 * <li>"{@code fooBar}" = "{@code foobar}"</li>
		 * <li>"{@code foo_bar}" = "{@code foobar}"</li>
		 * <li>"{@code [Foo.bar]}" = "{@code Foo.bar}"</li>
		 * </ul>
		 */
		UNIFORM

	}

	/**
	 * Internal functional interface used when processing names.
	 */
	@FunctionalInterface
	private interface ElementProcessor {

		void process(CharSequence elementValue, int start, int end, boolean indexed);

	}

	/**
	 * Internal filter used to strip out characters.
	 */
	private interface CharFilter {

		boolean isExcluded(char ch, int index);

	}

	/**
	 * Internal processor used to change characters.
	 */
	private interface CharProcessor {

		CharProcessor NONE = (c, i) -> c;

		CharProcessor LOWERCASE = (c, i) -> Character.toLowerCase(c);

		char process(char c, int index);

	}

	/**
	 * {@link ElementProcessor} that checks if a name is valid.
	 */
	private static class ElementValidator implements ElementProcessor {

		private boolean valid = true;

		@Override
		public void process(CharSequence elementValue, int start, int end,
				boolean indexed) {
			if (this.valid && !indexed) {
				this.valid = isValidElement(elementValue);
			}
		}

		public boolean isValid() {
			return this.valid;
		}

		public static boolean isValidElement(CharSequence elementValue) {
			return getInvalidChars(elementValue).isEmpty();
		}

		private static List<Character> getInvalidChars(CharSequence elementValue) {
			List<Character> chars = new ArrayList<>();
			for (int i = 0; i < elementValue.length(); i++) {
				char ch = elementValue.charAt(i);
				if (!isValidChar(ch, i)) {
					chars.add(ch);
				}
			}
			return chars;
		}

		public static boolean isValidChar(char ch, int index) {
			boolean isAlpha = ch >= 'a' && ch <= 'z';
			boolean isNumeric = ch >= '0' && ch <= '9';
			if (index == 0) {
				return isAlpha;
			}
			return isAlpha || isNumeric || ch == '-';
		}

	}

}
