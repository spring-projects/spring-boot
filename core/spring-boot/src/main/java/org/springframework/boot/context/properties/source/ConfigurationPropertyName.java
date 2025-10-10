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

package org.springframework.boot.context.properties.source;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.function.IntFunction;

import org.jspecify.annotations.Nullable;

import org.springframework.lang.Contract;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * A configuration property name composed of elements separated by dots. User created
 * names may contain the characters "{@code a-z}" "{@code 0-9}" and "{@code -}", they must
 * be lower-case and must start with an alphanumeric character. The "{@code -}" is used
 * purely for formatting, i.e. "{@code foo-bar}" and "{@code foobar}" are considered
 * equivalent.
 * <p>
 * The "{@code [}" and "{@code ]}" characters may be used to indicate an associative
 * index(i.e. a {@link Map} key or a {@link Collection} index). Indexes names are not
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
public final class ConfigurationPropertyName implements Comparable<ConfigurationPropertyName> {

	private static final String EMPTY_STRING = "";

	/**
	 * An empty {@link ConfigurationPropertyName}.
	 */
	public static final ConfigurationPropertyName EMPTY = new ConfigurationPropertyName(Elements.EMPTY);

	private final Elements elements;

	private final @Nullable CharSequence[] uniformElements;

	private int hashCode;

	private @Nullable String[] string = new String[ToStringFormat.values().length];

	private @Nullable Boolean hasDashedElement;

	private @Nullable ConfigurationPropertyName systemEnvironmentLegacyName;

	private ConfigurationPropertyName(Elements elements) {
		this.elements = elements;
		this.uniformElements = new CharSequence[elements.getSize()];
	}

	/**
	 * Returns {@code true} if this {@link ConfigurationPropertyName} is empty.
	 * @return {@code true} if the name is empty
	 */
	public boolean isEmpty() {
		return this.elements.getSize() == 0;
	}

	/**
	 * Return if the last element in the name is indexed.
	 * @return {@code true} if the last element is indexed
	 */
	public boolean isLastElementIndexed() {
		int size = getNumberOfElements();
		return (size > 0 && isIndexed(size - 1));
	}

	/**
	 * Return {@code true} if any element in the name is indexed.
	 * @return if the element has one or more indexed elements
	 * @since 2.2.10
	 */
	public boolean hasIndexedElement() {
		for (int i = 0; i < getNumberOfElements(); i++) {
			if (isIndexed(i)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Return if the element in the name is indexed.
	 * @param elementIndex the index of the element
	 * @return {@code true} if the element is indexed
	 */
	boolean isIndexed(int elementIndex) {
		return this.elements.getType(elementIndex).isIndexed();
	}

	/**
	 * Return if the element in the name is indexed and numeric.
	 * @param elementIndex the index of the element
	 * @return {@code true} if the element is indexed and numeric
	 */
	public boolean isNumericIndex(int elementIndex) {
		return this.elements.getType(elementIndex) == ElementType.NUMERICALLY_INDEXED;
	}

	/**
	 * Return the last element in the name in the given form.
	 * @param form the form to return
	 * @return the last element
	 */
	public String getLastElement(Form form) {
		int size = getNumberOfElements();
		return (size != 0) ? getElement(size - 1, form) : EMPTY_STRING;
	}

	/**
	 * Return an element in the name in the given form.
	 * @param elementIndex the element index
	 * @param form the form to return
	 * @return the last element
	 */
	public String getElement(int elementIndex, Form form) {
		CharSequence element = this.elements.get(elementIndex);
		ElementType type = this.elements.getType(elementIndex);
		if (type.isIndexed()) {
			return element.toString();
		}
		if (form == Form.ORIGINAL) {
			if (type != ElementType.NON_UNIFORM) {
				return element.toString();
			}
			return convertToOriginalForm(element).toString();
		}
		if (form == Form.DASHED) {
			if (type == ElementType.UNIFORM || type == ElementType.DASHED) {
				return element.toString();
			}
			return convertToDashedElement(element).toString();
		}
		CharSequence uniformElement = this.uniformElements[elementIndex];
		if (uniformElement == null) {
			uniformElement = (type != ElementType.UNIFORM) ? convertToUniformElement(element) : element;
			this.uniformElements[elementIndex] = uniformElement.toString();
		}
		return uniformElement.toString();
	}

	private CharSequence convertToOriginalForm(CharSequence element) {
		return convertElement(element, false,
				(ch, i) -> ch == '_' || ElementsParser.isValidChar(Character.toLowerCase(ch), i));
	}

	private CharSequence convertToDashedElement(CharSequence element) {
		return convertElement(element, true, ElementsParser::isValidChar);
	}

	private CharSequence convertToUniformElement(CharSequence element) {
		return convertElement(element, true, (ch, i) -> ElementsParser.isAlphaNumeric(ch));
	}

	private CharSequence convertElement(CharSequence element, boolean lowercase, ElementCharPredicate filter) {
		StringBuilder result = new StringBuilder(element.length());
		for (int i = 0; i < element.length(); i++) {
			char ch = lowercase ? Character.toLowerCase(element.charAt(i)) : element.charAt(i);
			if (filter.test(ch, i)) {
				result.append(ch);
			}
		}
		return result;
	}

	/**
	 * Return the total number of elements in the name.
	 * @return the number of elements
	 */
	public int getNumberOfElements() {
		return this.elements.getSize();
	}

	/**
	 * Create a new {@link ConfigurationPropertyName} by appending the given suffix.
	 * @param suffix the elements to append
	 * @return a new {@link ConfigurationPropertyName}
	 * @throws InvalidConfigurationPropertyNameException if the result is not valid
	 */
	public ConfigurationPropertyName append(@Nullable String suffix) {
		if (!StringUtils.hasLength(suffix)) {
			return this;
		}
		Elements additionalElements = probablySingleElementOf(suffix);
		return new ConfigurationPropertyName(this.elements.append(additionalElements));
	}

	/**
	 * Create a new {@link ConfigurationPropertyName} by appending the given suffix.
	 * @param suffix the elements to append
	 * @return a new {@link ConfigurationPropertyName}
	 * @since 2.5.0
	 */
	public ConfigurationPropertyName append(@Nullable ConfigurationPropertyName suffix) {
		if (suffix == null) {
			return this;
		}
		return new ConfigurationPropertyName(this.elements.append(suffix.elements));
	}

	/**
	 * Return the parent of this {@link ConfigurationPropertyName} or
	 * {@link ConfigurationPropertyName#EMPTY} if there is no parent.
	 * @return the parent name
	 */
	public ConfigurationPropertyName getParent() {
		int numberOfElements = getNumberOfElements();
		return (numberOfElements <= 1) ? EMPTY : chop(numberOfElements - 1);
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
		return new ConfigurationPropertyName(this.elements.chop(size));
	}

	/**
	 * Return a new {@link ConfigurationPropertyName} by based on this name offset by
	 * specific element index. For example, {@code subName(1)} on the name {@code foo.bar}
	 * will return {@code bar}.
	 * @param offset the element offset
	 * @return the sub name
	 * @since 2.5.0
	 */
	public ConfigurationPropertyName subName(int offset) {
		if (offset == 0) {
			return this;
		}
		if (offset == getNumberOfElements()) {
			return EMPTY;
		}
		if (offset < 0 || offset > getNumberOfElements()) {
			throw new IndexOutOfBoundsException("Offset: " + offset + ", NumberOfElements: " + getNumberOfElements());
		}
		return new ConfigurationPropertyName(this.elements.subElements(offset));
	}

	/**
	 * Returns {@code true} if this element is an immediate parent of the specified name.
	 * @param name the name to check
	 * @return {@code true} if this name is an ancestor
	 */
	public boolean isParentOf(ConfigurationPropertyName name) {
		Assert.notNull(name, "'name' must not be null");
		if (getNumberOfElements() != name.getNumberOfElements() - 1) {
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
		Assert.notNull(name, "'name' must not be null");
		if (getNumberOfElements() >= name.getNumberOfElements()) {
			return false;
		}
		return endsWithElementsEqualTo(name);
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
			try {
				ElementType type1 = (i1 < l1) ? n1.elements.getType(i1) : null;
				ElementType type2 = (i2 < l2) ? n2.elements.getType(i2) : null;
				String e1 = (i1 < l1) ? n1.getElement(i1++, Form.UNIFORM) : null;
				String e2 = (i2 < l2) ? n2.getElement(i2++, Form.UNIFORM) : null;
				int result = compare(e1, type1, e2, type2);
				if (result != 0) {
					return result;
				}
			}
			catch (ArrayIndexOutOfBoundsException ex) {
				throw new RuntimeException(ex);
			}
		}
		return 0;
	}

	private int compare(@Nullable String e1, @Nullable ElementType type1, @Nullable String e2,
			@Nullable ElementType type2) {
		if (e1 == null) {
			return -1;
		}
		if (e2 == null) {
			return 1;
		}
		Assert.state(type1 != null, "'type1' must not be null");
		Assert.state(type2 != null, "'type2' must not be null");
		int result = Boolean.compare(type2.isIndexed(), type1.isIndexed());
		if (result != 0) {
			return result;
		}
		if (type1 == ElementType.NUMERICALLY_INDEXED && type2 == ElementType.NUMERICALLY_INDEXED) {
			long v1 = Long.parseLong(e1);
			long v2 = Long.parseLong(e2);
			return Long.compare(v1, v2);
		}
		return e1.compareTo(e2);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (obj == null || obj.getClass() != getClass()) {
			return false;
		}
		ConfigurationPropertyName other = (ConfigurationPropertyName) obj;
		if (getNumberOfElements() != other.getNumberOfElements()) {
			return false;
		}
		if (this.elements.canShortcutWithSource(ElementType.UNIFORM)
				&& other.elements.canShortcutWithSource(ElementType.UNIFORM)) {
			return toString().equals(other.toString());
		}
		if (hashCode() != other.hashCode()) {
			return false;
		}
		if (toStringMatches(toString(), other.toString())) {
			return true;
		}
		return endsWithElementsEqualTo(other);
	}

	private boolean toStringMatches(String s1, String s2) {
		return s1.hashCode() == s2.hashCode() && s1.equals(s2);
	}

	private boolean endsWithElementsEqualTo(ConfigurationPropertyName name) {
		for (int i = this.elements.getSize() - 1; i >= 0; i--) {
			if (elementDiffers(this.elements, name.elements, i)) {
				return false;
			}
		}
		return true;
	}

	private boolean elementDiffers(Elements e1, Elements e2, int i) {
		ElementType type1 = e1.getType(i);
		ElementType type2 = e2.getType(i);
		if (type1.allowsFastEqualityCheck() && type2.allowsFastEqualityCheck()) {
			return !fastElementEquals(e1, e2, i);
		}
		if (type1.allowsDashIgnoringEqualityCheck() && type2.allowsDashIgnoringEqualityCheck()) {
			return !dashIgnoringElementEquals(e1, e2, i);
		}
		return !defaultElementEquals(e1, e2, i);
	}

	private boolean fastElementEquals(Elements e1, Elements e2, int i) {
		int length1 = e1.getLength(i);
		int length2 = e2.getLength(i);
		if (length1 == length2) {
			int i1 = 0;
			while (length1-- != 0) {
				char ch1 = e1.charAt(i, i1);
				char ch2 = e2.charAt(i, i1);
				if (ch1 != ch2) {
					return false;
				}
				i1++;
			}
			return true;
		}
		return false;
	}

	private boolean dashIgnoringElementEquals(Elements e1, Elements e2, int i) {
		int l1 = e1.getLength(i);
		int l2 = e2.getLength(i);
		int i1 = 0;
		int i2 = 0;
		while (i1 < l1) {
			if (i2 >= l2) {
				return remainderIsDashes(e1, i, i1);
			}
			char ch1 = e1.charAt(i, i1);
			char ch2 = e2.charAt(i, i2);
			if (ch1 == '-') {
				i1++;
			}
			else if (ch2 == '-') {
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
		if (i2 < l2) {
			if (e2.getType(i).isIndexed()) {
				return false;
			}
			do {
				char ch2 = e2.charAt(i, i2++);
				if (ch2 != '-') {
					return false;
				}
			}
			while (i2 < l2);
		}
		return true;
	}

	private boolean defaultElementEquals(Elements e1, Elements e2, int i) {
		int l1 = e1.getLength(i);
		int l2 = e2.getLength(i);
		boolean indexed1 = e1.getType(i).isIndexed();
		boolean indexed2 = e2.getType(i).isIndexed();
		int i1 = 0;
		int i2 = 0;
		while (i1 < l1) {
			if (i2 >= l2) {
				return remainderIsNotAlphanumeric(e1, i, i1);
			}
			char ch1 = indexed1 ? e1.charAt(i, i1) : Character.toLowerCase(e1.charAt(i, i1));
			char ch2 = indexed2 ? e2.charAt(i, i2) : Character.toLowerCase(e2.charAt(i, i2));
			if (!indexed1 && !ElementsParser.isAlphaNumeric(ch1)) {
				i1++;
			}
			else if (!indexed2 && !ElementsParser.isAlphaNumeric(ch2)) {
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
		if (i2 < l2) {
			return remainderIsNotAlphanumeric(e2, i, i2);
		}
		return true;
	}

	private boolean remainderIsNotAlphanumeric(Elements elements, int element, int index) {
		if (elements.getType(element).isIndexed()) {
			return false;
		}
		int length = elements.getLength(element);
		do {
			char c = Character.toLowerCase(elements.charAt(element, index++));
			if (ElementsParser.isAlphaNumeric(c)) {
				return false;
			}
		}
		while (index < length);
		return true;
	}

	private boolean remainderIsDashes(Elements elements, int element, int index) {
		if (elements.getType(element).isIndexed()) {
			return false;
		}
		int length = elements.getLength(element);
		do {
			char c = elements.charAt(element, index++);
			if (c != '-') {
				return false;
			}
		}
		while (index < length);
		return true;
	}

	@Override
	public int hashCode() {
		int hashCode = this.hashCode;
		Elements elements = this.elements;
		if (hashCode == 0 && elements.getSize() != 0) {
			for (int elementIndex = 0; elementIndex < elements.getSize(); elementIndex++) {
				hashCode = 31 * hashCode + elements.hashCode(elementIndex);
			}
			this.hashCode = hashCode;
		}
		return hashCode;
	}

	@Nullable ConfigurationPropertyName asSystemEnvironmentLegacyName() {
		ConfigurationPropertyName name = this.systemEnvironmentLegacyName;
		if (name == null) {
			name = ConfigurationPropertyName
				.ofIfValid(buildSimpleToString('.', (i) -> getElement(i, Form.DASHED).replace('-', '.')));
			this.systemEnvironmentLegacyName = (name != null) ? name : EMPTY;
		}
		return (name != EMPTY) ? name : null;
	}

	@Override
	public String toString() {
		return toString(ToStringFormat.DEFAULT, false);
	}

	String toString(ToStringFormat format, boolean upperCase) {
		String string = this.string[format.ordinal()];
		if (string == null) {
			string = buildToString(format);
			this.string[format.ordinal()] = string;
		}
		return (!upperCase) ? string : string.toUpperCase(Locale.ENGLISH);
	}

	private String buildToString(ToStringFormat format) {
		return switch (format) {
			case DEFAULT -> buildDefaultToString();
			case SYSTEM_ENVIRONMENT -> buildSimpleToString('_', (i) -> getElement(i, Form.UNIFORM));
			case LEGACY_SYSTEM_ENVIRONMENT ->
				buildSimpleToString('_', (i) -> getElement(i, Form.ORIGINAL).replace('-', '_'));
		};
	}

	private String buildDefaultToString() {
		if (this.elements.canShortcutWithSource(ElementType.UNIFORM, ElementType.DASHED)) {
			return this.elements.getSource().toString();
		}
		int elements = getNumberOfElements();
		StringBuilder result = new StringBuilder(elements * 8);
		for (int i = 0; i < elements; i++) {
			boolean indexed = isIndexed(i);
			if (!result.isEmpty() && !indexed) {
				result.append('.');
			}
			if (indexed) {
				result.append('[');
				result.append(getElement(i, Form.ORIGINAL));
				result.append(']');
			}
			else {
				result.append(getElement(i, Form.DASHED));
			}
		}
		return result.toString();
	}

	private String buildSimpleToString(char joinChar, IntFunction<String> elementConverter) {
		StringBuilder result = new StringBuilder();
		for (int i = 0; i < getNumberOfElements(); i++) {
			if (!result.isEmpty()) {
				result.append(joinChar);
			}
			result.append(elementConverter.apply(i));
		}
		return result.toString();
	}

	boolean hasDashedElement() {
		Boolean hasDashedElement = this.hasDashedElement;
		if (hasDashedElement != null) {
			return hasDashedElement;
		}
		for (int i = 0; i < getNumberOfElements(); i++) {
			if (getElement(i, Form.DASHED).indexOf('-') != -1) {
				this.hasDashedElement = true;
				return true;
			}
		}
		this.hasDashedElement = false;
		return false;
	}

	/**
	 * Returns if the given name is valid. If this method returns {@code true} then the
	 * name may be used with {@link #of(CharSequence)} without throwing an exception.
	 * @param name the name to test
	 * @return {@code true} if the name is valid
	 */
	public static boolean isValid(@Nullable CharSequence name) {
		return of(name, true) != null;
	}

	/**
	 * Return a {@link ConfigurationPropertyName} for the specified string.
	 * @param name the source name
	 * @return a {@link ConfigurationPropertyName} instance
	 * @throws InvalidConfigurationPropertyNameException if the name is not valid
	 */
	@SuppressWarnings("NullAway") // See https://github.com/uber/NullAway/issues/1232
	public static ConfigurationPropertyName of(@Nullable CharSequence name) {
		return of(name, false);
	}

	/**
	 * Return a {@link ConfigurationPropertyName} for the specified string or {@code null}
	 * if the name is not valid.
	 * @param name the source name
	 * @return a {@link ConfigurationPropertyName} instance
	 * @since 2.3.1
	 */
	public static @Nullable ConfigurationPropertyName ofIfValid(@Nullable CharSequence name) {
		return of(name, true);
	}

	/**
	 * Return a {@link ConfigurationPropertyName} for the specified string.
	 * @param name the source name
	 * @param returnNullIfInvalid if null should be returned if the name is not valid
	 * @return a {@link ConfigurationPropertyName} instance
	 * @throws InvalidConfigurationPropertyNameException if the name is not valid and
	 * {@code returnNullIfInvalid} is {@code false}
	 */
	@Contract("_, false -> !null")
	static @Nullable ConfigurationPropertyName of(@Nullable CharSequence name, boolean returnNullIfInvalid) {
		Elements elements = elementsOf(name, returnNullIfInvalid, ElementsParser.DEFAULT_CAPACITY);
		return (elements != null) ? new ConfigurationPropertyName(elements) : null;
	}

	@SuppressWarnings("NullAway") // See https://github.com/uber/NullAway/issues/1232
	private static Elements probablySingleElementOf(CharSequence name) {
		return elementsOf(name, false, 1);
	}

	@Contract("_, false, _ -> !null")
	private static @Nullable Elements elementsOf(@Nullable CharSequence name, boolean returnNullIfInvalid,
			int parserCapacity) {
		if (name == null) {
			Assert.isTrue(returnNullIfInvalid, "'name' must not be null");
			return null;
		}
		if (name.isEmpty()) {
			return Elements.EMPTY;
		}
		if (name.charAt(0) == '.' || name.charAt(name.length() - 1) == '.') {
			if (returnNullIfInvalid) {
				return null;
			}
			throw new InvalidConfigurationPropertyNameException(name, Collections.singletonList('.'));
		}
		Elements elements = new ElementsParser(name, '.', parserCapacity).parse();
		for (int i = 0; i < elements.getSize(); i++) {
			if (elements.getType(i) == ElementType.NON_UNIFORM) {
				if (returnNullIfInvalid) {
					return null;
				}
				throw new InvalidConfigurationPropertyNameException(name, getInvalidChars(elements, i));
			}
		}
		return elements;
	}

	private static List<Character> getInvalidChars(Elements elements, int index) {
		List<Character> invalidChars = new ArrayList<>();
		for (int charIndex = 0; charIndex < elements.getLength(index); charIndex++) {
			char ch = elements.charAt(index, charIndex);
			if (!ElementsParser.isValidChar(ch, charIndex)) {
				invalidChars.add(ch);
			}
		}
		return invalidChars;
	}

	/**
	 * Create a {@link ConfigurationPropertyName} by adapting the given source. See
	 * {@link #adapt(CharSequence, char, Function)} for details.
	 * @param name the name to parse
	 * @param separator the separator used to split the name
	 * @return a {@link ConfigurationPropertyName}
	 */
	public static ConfigurationPropertyName adapt(CharSequence name, char separator) {
		return adapt(name, separator, null);
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
			@Nullable Function<CharSequence, CharSequence> elementValueProcessor) {
		Assert.notNull(name, "Name must not be null");
		if (name.isEmpty()) {
			return EMPTY;
		}
		Elements elements = new ElementsParser(name, separator).parse(elementValueProcessor);
		if (elements.getSize() == 0) {
			return EMPTY;
		}
		return new ConfigurationPropertyName(elements);
	}

	/**
	 * The various forms that a non-indexed element value can take.
	 */
	public enum Form {

		/**
		 * The original form as specified when the name was created or adapted. For
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
		 * The dashed configuration form (used for toString; lower-case with only
		 * alphanumeric characters and dashes).
		 * <ul>
		 * <li>"{@code foo-bar}" = "{@code foo-bar}"</li>
		 * <li>"{@code fooBar}" = "{@code foobar}"</li>
		 * <li>"{@code foo_bar}" = "{@code foobar}"</li>
		 * <li>"{@code [Foo.bar]}" = "{@code Foo.bar}"</li>
		 * </ul>
		 */
		DASHED,

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
	 * Allows access to the individual elements that make up the name. We store the
	 * indexes in arrays rather than a list of object in order to conserve memory.
	 */
	private static class Elements {

		private static final int[] NO_POSITION = {};

		private static final ElementType[] NO_TYPE = {};

		public static final Elements EMPTY = new Elements("", 0, NO_POSITION, NO_POSITION, NO_TYPE, null, null);

		private final CharSequence source;

		private final int size;

		private final int[] start;

		private final int[] end;

		private final ElementType[] type;

		private final int[] hashCode;

		/**
		 * Contains any resolved elements or can be {@code null} if there aren't any.
		 * Resolved elements allow us to modify the element values in some way (or example
		 * when adapting with a mapping function, or when append has been called). Note
		 * that this array is not used as a cache, in fact, when it's not null then
		 * {@link #canShortcutWithSource} will always return false which may hurt
		 * performance.
		 */
		private final @Nullable CharSequence @Nullable [] resolved;

		Elements(CharSequence source, int size, int[] start, int[] end, ElementType[] type, int @Nullable [] hashCode,
				CharSequence @Nullable [] resolved) {
			this.source = source;
			this.size = size;
			this.start = start;
			this.end = end;
			this.type = type;
			this.hashCode = (hashCode != null) ? hashCode : new int[size];
			this.resolved = resolved;
		}

		Elements append(Elements additional) {
			int size = this.size + additional.size;
			ElementType[] type = new ElementType[size];
			int[] hashCode = new int[size];
			System.arraycopy(this.type, 0, type, 0, this.size);
			System.arraycopy(additional.type, 0, type, this.size, additional.size);
			System.arraycopy(this.hashCode, 0, hashCode, 0, this.size);
			System.arraycopy(additional.hashCode, 0, hashCode, this.size, additional.size);
			CharSequence[] resolved = newResolved(0, size);
			for (int i = 0; i < additional.size; i++) {
				resolved[this.size + i] = additional.get(i);
			}
			return new Elements(this.source, size, this.start, this.end, type, hashCode, resolved);
		}

		Elements chop(int size) {
			CharSequence[] resolved = newResolved(0, size);
			return new Elements(this.source, size, this.start, this.end, this.type, this.hashCode, resolved);
		}

		Elements subElements(int offset) {
			int size = this.size - offset;
			CharSequence[] resolved = newResolved(offset, size);
			int[] start = new int[size];
			System.arraycopy(this.start, offset, start, 0, size);
			int[] end = new int[size];
			System.arraycopy(this.end, offset, end, 0, size);
			ElementType[] type = new ElementType[size];
			System.arraycopy(this.type, offset, type, 0, size);
			int[] hashCode = new int[size];
			System.arraycopy(this.hashCode, offset, hashCode, 0, size);
			return new Elements(this.source, size, start, end, type, hashCode, resolved);
		}

		private CharSequence[] newResolved(int offset, int size) {
			CharSequence[] resolved = new CharSequence[size];
			if (this.resolved != null) {
				System.arraycopy(this.resolved, offset, resolved, 0, Math.min(size, this.size));
			}
			return resolved;
		}

		int getSize() {
			return this.size;
		}

		CharSequence get(int index) {
			if (this.resolved != null) {
				CharSequence element = this.resolved[index];
				if (element != null) {
					return element;
				}
			}
			int start = this.start[index];
			int end = this.end[index];
			return this.source.subSequence(start, end);
		}

		int getLength(int index) {
			if (this.resolved != null) {
				CharSequence element = this.resolved[index];
				if (element != null) {
					return element.length();
				}
			}
			int start = this.start[index];
			int end = this.end[index];
			return end - start;
		}

		char charAt(int index, int charIndex) {
			if (this.resolved != null) {
				CharSequence element = this.resolved[index];
				if (element != null) {
					return element.charAt(charIndex);
				}
			}
			int start = this.start[index];
			return this.source.charAt(start + charIndex);
		}

		ElementType getType(int index) {
			return this.type[index];
		}

		int hashCode(int index) {
			int hashCode = this.hashCode[index];
			if (hashCode == 0) {
				boolean indexed = getType(index).isIndexed();
				int length = getLength(index);
				for (int i = 0; i < length; i++) {
					char ch = charAt(index, i);
					if (!indexed) {
						ch = Character.toLowerCase(ch);
					}
					if (ElementsParser.isAlphaNumeric(ch)) {
						hashCode = 31 * hashCode + ch;
					}
				}
				this.hashCode[index] = hashCode;
			}
			return hashCode;
		}

		CharSequence getSource() {
			return this.source;
		}

		/**
		 * Returns if the element source can be used as a shortcut for an operation such
		 * as {@code equals} or {@code toString}.
		 * @param requiredType the required type
		 * @return {@code true} if all elements match at least one of the types
		 */
		boolean canShortcutWithSource(ElementType requiredType) {
			return canShortcutWithSource(requiredType, requiredType);
		}

		/**
		 * Returns if the element source can be used as a shortcut for an operation such
		 * as {@code equals} or {@code toString}.
		 * @param requiredType the required type
		 * @param alternativeType and alternative required type
		 * @return {@code true} if all elements match at least one of the types
		 */
		boolean canShortcutWithSource(ElementType requiredType, ElementType alternativeType) {
			if (this.resolved != null) {
				return false;
			}
			for (int i = 0; i < this.size; i++) {
				ElementType type = this.type[i];
				if (type != requiredType && type != alternativeType) {
					return false;
				}
				if (i > 0 && this.end[i - 1] + 1 != this.start[i]) {
					return false;
				}
			}
			return true;
		}

	}

	/**
	 * Main parsing logic used to convert a {@link CharSequence} to {@link Elements}.
	 */
	private static class ElementsParser {

		private static final int DEFAULT_CAPACITY = 6;

		private final CharSequence source;

		private final char separator;

		private int size;

		private int[] start;

		private int[] end;

		private ElementType[] type;

		private CharSequence @Nullable [] resolved;

		ElementsParser(CharSequence source, char separator) {
			this(source, separator, DEFAULT_CAPACITY);
		}

		ElementsParser(CharSequence source, char separator, int capacity) {
			this.source = source;
			this.separator = separator;
			this.start = new int[capacity];
			this.end = new int[capacity];
			this.type = new ElementType[capacity];
		}

		Elements parse() {
			return parse(null);
		}

		Elements parse(@Nullable Function<CharSequence, CharSequence> valueProcessor) {
			int length = this.source.length();
			int openBracketCount = 0;
			int start = 0;
			ElementType type = ElementType.EMPTY;
			for (int i = 0; i < length; i++) {
				char ch = this.source.charAt(i);
				if (ch == '[') {
					if (openBracketCount == 0) {
						add(start, i, type, valueProcessor);
						start = i + 1;
						type = ElementType.NUMERICALLY_INDEXED;
					}
					openBracketCount++;
				}
				else if (ch == ']') {
					openBracketCount--;
					if (openBracketCount == 0) {
						add(start, i, type, valueProcessor);
						start = i + 1;
						type = ElementType.EMPTY;
					}
				}
				else if (!type.isIndexed() && ch == this.separator) {
					add(start, i, type, valueProcessor);
					start = i + 1;
					type = ElementType.EMPTY;
				}
				else {
					type = updateType(type, ch, i - start);
				}
			}
			if (openBracketCount != 0) {
				type = ElementType.NON_UNIFORM;
			}
			add(start, length, type, valueProcessor);
			return new Elements(this.source, this.size, this.start, this.end, this.type, null, this.resolved);
		}

		private ElementType updateType(ElementType existingType, char ch, int index) {
			if (existingType.isIndexed()) {
				if (existingType == ElementType.NUMERICALLY_INDEXED && !isNumeric(ch)) {
					return ElementType.INDEXED;
				}
				return existingType;
			}
			if (existingType == ElementType.EMPTY && isValidChar(ch, index)) {
				return (index == 0) ? ElementType.UNIFORM : ElementType.NON_UNIFORM;
			}
			if (existingType == ElementType.UNIFORM && ch == '-') {
				return ElementType.DASHED;
			}
			if (!isValidChar(ch, index)) {
				if (existingType == ElementType.EMPTY && !isValidChar(Character.toLowerCase(ch), index)) {
					return ElementType.EMPTY;
				}
				return ElementType.NON_UNIFORM;
			}
			return existingType;
		}

		private void add(int start, int end, ElementType type,
				@Nullable Function<CharSequence, CharSequence> valueProcessor) {
			if ((end - start) < 1 || type == ElementType.EMPTY) {
				return;
			}
			if (this.start.length == this.size) {
				this.start = expand(this.start);
				this.end = expand(this.end);
				this.type = expand(this.type);
				this.resolved = expand(this.resolved);
			}
			if (valueProcessor != null) {
				if (this.resolved == null) {
					this.resolved = new CharSequence[this.start.length];
				}
				CharSequence resolved = valueProcessor.apply(this.source.subSequence(start, end));
				Elements resolvedElements = new ElementsParser(resolved, '.').parse();
				Assert.state(resolvedElements.getSize() == 1, "Resolved element must not contain multiple elements");
				this.resolved[this.size] = resolvedElements.get(0);
				type = resolvedElements.getType(0);
			}
			this.start[this.size] = start;
			this.end[this.size] = end;
			this.type[this.size] = type;
			this.size++;
		}

		private int[] expand(int[] src) {
			int[] dest = new int[src.length + DEFAULT_CAPACITY];
			System.arraycopy(src, 0, dest, 0, src.length);
			return dest;
		}

		private ElementType[] expand(ElementType[] src) {
			ElementType[] dest = new ElementType[src.length + DEFAULT_CAPACITY];
			System.arraycopy(src, 0, dest, 0, src.length);
			return dest;
		}

		private CharSequence @Nullable [] expand(CharSequence @Nullable [] src) {
			if (src == null) {
				return null;
			}
			CharSequence[] dest = new CharSequence[src.length + DEFAULT_CAPACITY];
			System.arraycopy(src, 0, dest, 0, src.length);
			return dest;
		}

		static boolean isValidChar(char ch, int index) {
			return isAlpha(ch) || isNumeric(ch) || (index != 0 && ch == '-');
		}

		static boolean isAlphaNumeric(char ch) {
			return isAlpha(ch) || isNumeric(ch);
		}

		private static boolean isAlpha(char ch) {
			return ch >= 'a' && ch <= 'z';
		}

		private static boolean isNumeric(char ch) {
			return ch >= '0' && ch <= '9';
		}

	}

	/**
	 * The various types of element that we can detect.
	 */
	private enum ElementType {

		/**
		 * The element is logically empty (contains no valid chars).
		 */
		EMPTY(false),

		/**
		 * The element is a uniform name (a-z, 0-9, no dashes, lowercase).
		 */
		UNIFORM(false),

		/**
		 * The element is almost uniform, but it contains (but does not start with) at
		 * least one dash.
		 */
		DASHED(false),

		/**
		 * The element contains non-uniform characters and will need to be converted.
		 */
		NON_UNIFORM(false),

		/**
		 * The element is non-numerically indexed.
		 */
		INDEXED(true),

		/**
		 * The element is numerically indexed.
		 */
		NUMERICALLY_INDEXED(true);

		private final boolean indexed;

		ElementType(boolean indexed) {
			this.indexed = indexed;
		}

		public boolean isIndexed() {
			return this.indexed;
		}

		public boolean allowsFastEqualityCheck() {
			return this == UNIFORM || this == NUMERICALLY_INDEXED;
		}

		public boolean allowsDashIgnoringEqualityCheck() {
			return allowsFastEqualityCheck() || this == DASHED;
		}

	}

	/**
	 * Predicate used to filter element chars.
	 */
	private interface ElementCharPredicate {

		boolean test(char ch, int index);

	}

	/**
	 * Formats for {@code toString}.
	 */
	enum ToStringFormat {

		DEFAULT, SYSTEM_ENVIRONMENT, LEGACY_SYSTEM_ENVIRONMENT

	}

}
