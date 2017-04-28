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

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.springframework.boot.context.properties.source.ConfigurationPropertyName.Element;
import org.springframework.boot.context.properties.source.ConfigurationPropertyNameBuilder.ElementValueProcessor;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * A configuration property name composed of elements separated by dots. Names may contain
 * the characters "{@code a-z}" "{@code 0-9}") and "{@code -}", they must be lower-case
 * and must start with a letter. The "{@code -}" is used purely for formatting, i.e.
 * "{@code foo-bar}" and "{@code foobar}" are considered equivalent.
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
 * <p>
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @since 2.0.0
 * @see #of(String)
 * @see ConfigurationPropertyNameBuilder
 * @see ConfigurationPropertySource
 */
public final class ConfigurationPropertyName
		implements Iterable<Element>, Comparable<ConfigurationPropertyName> {

	/**
	 * An empty {@link ConfigurationPropertyName}.
	 */
	public static final ConfigurationPropertyName EMPTY = new ConfigurationPropertyName(
			null, new Element());

	private static final ConfigurationPropertyNameBuilder BUILDER = new ConfigurationPropertyNameBuilder(
			ElementValueProcessor.identity().withValidName());

	private final ConfigurationPropertyName parent;

	private final Element element;

	private String toString;

	ConfigurationPropertyName(ConfigurationPropertyName parent, Element element) {
		Assert.notNull(element, "Element must not be null");
		this.parent = (parent == EMPTY ? null : parent);
		this.element = element;
	}

	/**
	 * Return the parent of this configuration property.
	 * @return the parent or {code null}
	 */
	public ConfigurationPropertyName getParent() {
		return this.parent;
	}

	/**
	 * Return the element part of this configuration property name.
	 * @return the element (never {@code null})
	 */
	public Element getElement() {
		return this.element;
	}

	@Override
	public Iterator<Element> iterator() {
		return stream().iterator();
	}

	/**
	 * Return a stream of the {@link Element Elements} that make up this name.
	 * @return a stream of {@link Element} items
	 */
	public Stream<Element> stream() {
		if (this.parent == null) {
			return Stream.of(this.element);
		}
		return Stream.concat(this.parent.stream(), Stream.of(this.element));
	}

	/**
	 * Return a stream of the {@link Element Elements} that make up this name starting
	 * from the given root.
	 * @param root the root of the name or {@code null} to stream all elements
	 * @return a stream of {@link Element} items
	 */
	public Stream<Element> stream(ConfigurationPropertyName root) {
		if (this.parent == null || this.parent.equals(root)) {
			return Stream.of(this.element);
		}
		return Stream.concat(this.parent.stream(root), Stream.of(this.element));
	}

	@Override
	public String toString() {
		if (this.toString == null) {
			this.toString = buildToString();
		}
		return this.toString;
	}

	private String buildToString() {
		StringBuilder result = new StringBuilder();
		result.append(this.parent != null ? this.parent.toString() : "");
		result.append(result.length() > 0 && !this.element.isIndexed() ? "." : "");
		result.append(this.element);
		return result.toString();
	}

	/**
	 * Returns {@code true} if this element is an ancestor (immediate or nested parent) or
	 * the specified name.
	 * @param name the name to check
	 * @return {@code true} if this name is an ancestor
	 */
	public boolean isAncestorOf(ConfigurationPropertyName name) {
		ConfigurationPropertyName candidate = (name == null ? null : name.getParent());
		while (candidate != null) {
			if (candidate.equals(this)) {
				return true;
			}
			candidate = candidate.getParent();
		}
		return false;
	}

	@Override
	public int compareTo(ConfigurationPropertyName other) {
		Iterator<Element> elements = iterator();
		Iterator<Element> otherElements = other.iterator();
		while (elements.hasNext() || otherElements.hasNext()) {
			int result = compare(elements.hasNext() ? elements.next() : null,
					otherElements.hasNext() ? otherElements.next() : null);
			if (result != 0) {
				return result;
			}
		}
		return 0;
	}

	private int compare(Element element, Element other) {
		if (element == null) {
			return -1;
		}
		if (other == null) {
			return 1;
		}
		return element.compareTo(other);
	}

	@Override
	public int hashCode() {
		int result = 1;
		result = 31 * result + ObjectUtils.nullSafeHashCode(this.parent);
		result = 31 * result + ObjectUtils.nullSafeHashCode(this.element);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		ConfigurationPropertyName other = (ConfigurationPropertyName) obj;
		boolean result = true;
		result = result && ObjectUtils.nullSafeEquals(this.parent, other.parent);
		result = result && ObjectUtils.nullSafeEquals(this.element, other.element);
		return result;
	}

	/**
	 * Create a new {@link ConfigurationPropertyName} by appending the given index.
	 * @param index the index to append
	 * @return a new {@link ConfigurationPropertyName}
	 */
	public ConfigurationPropertyName appendIndex(int index) {
		return BUILDER.from(this, index);
	}

	/**
	 * Create a new {@link ConfigurationPropertyName} by appending the given element.
	 * @param element the element to append
	 * @return a new {@link ConfigurationPropertyName}
	 */
	public ConfigurationPropertyName append(String element) {
		if (StringUtils.hasLength(element)) {
			return BUILDER.from(this, element);
		}
		return this;
	}

	/**
	 * Return a {@link ConfigurationPropertyName} for the specified string.
	 * @param name the source name
	 * @return a {@link ConfigurationPropertyName} instance
	 * @throws IllegalArgumentException if the name is not valid
	 */
	public static ConfigurationPropertyName of(String name)
			throws IllegalArgumentException {
		Assert.notNull(name, "Name must not be null");
		Assert.isTrue(!name.startsWith("."), "Name must not start with '.'");
		Assert.isTrue(!name.endsWith("."), "Name must not end with '.'");
		if (StringUtils.isEmpty(name)) {
			return EMPTY;
		}
		return BUILDER.from(name, '.');
	}

	/**
	 * An individual element of the {@link ConfigurationPropertyName}.
	 */
	public static final class Element implements Comparable<Element> {

		private static final Pattern VALUE_PATTERN = Pattern.compile("[\\w\\-]+");

		private final boolean indexed;

		private final String[] value;

		private Element() {
			this.indexed = false;
			this.value = Form.expand("", false);
		}

		Element(String value) {
			Assert.notNull(value, "Value must not be null");
			this.indexed = isIndexed(value);
			value = (this.indexed ? value.substring(1, value.length() - 1) : value);
			if (!this.indexed) {
				validate(value);
			}
			this.value = Form.expand(value, this.indexed);
		}

		private void validate(String value) {
			Assert.isTrue(VALUE_PATTERN.matcher(value).matches(),
					"Element value '" + value + "' is not valid");
		}

		@Override
		public int compareTo(Element other) {
			int result = Boolean.compare(other.indexed, this.indexed);
			if (result != 0) {
				return result;
			}
			if (this.indexed && other.indexed) {
				try {
					long value = Long.parseLong(getValue(Form.UNIFORM));
					long otherValue = Long.parseLong(other.getValue(Form.UNIFORM));
					return Long.compare(value, otherValue);
				}
				catch (NumberFormatException ex) {
					// Fallback to string comparison
				}
			}
			return getValue(Form.UNIFORM).compareTo(other.getValue(Form.UNIFORM));
		}

		@Override
		public int hashCode() {
			return getValue(Form.UNIFORM).hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null || getClass() != obj.getClass()) {
				return false;
			}
			return ObjectUtils.nullSafeEquals(getValue(Form.UNIFORM),
					((Element) obj).getValue(Form.UNIFORM));
		}

		@Override
		public String toString() {
			String string = getValue(Form.CONFIGURATION);
			return (this.indexed ? "[" + string + "]" : string);
		}

		/**
		 * Return if the element is indexed (i.e. should be displayed in angle brackets).
		 * @return if the element is indexed
		 */
		public boolean isIndexed() {
			return this.indexed;
		}

		/**
		 * Return the element value in the specified form. Indexed values (the part within
		 * square brackets) are always returned unchanged.
		 * @param form the form the value should take
		 * @return the value
		 */
		public String getValue(Form form) {
			form = (form != null ? form : Form.ORIGINAL);
			return this.value[form.ordinal()];
		}

		public static boolean isIndexed(String value) {
			return value.startsWith("[") && value.endsWith("]");
		}

	}

	/**
	 * The various forms that a non-indexed {@link Element} {@code value} can take.
	 */
	public enum Form {

		/**
		 * The original form as specified when the name was created. For example:
		 * <ul>
		 * <li>"{@code foo-bar}" = "{@code foo-bar}"</li>
		 * <li>"{@code fooBar}" = "{@code fooBar}"</li>
		 * <li>"{@code foo_bar}" = "{@code foo_bar}"</li>
		 * <li>"{@code [Foo.bar]}" = "{@code Foo.bar}"</li>
		 * </ul>
		 */
		ORIGINAL {

			@Override
			protected String convert(String value) {
				return value;
			}

		},

		/**
		 * The canonical configuration form (lower-case with only alphanumeric and
		 * "{@code -}" characters).
		 * <ul>
		 * <li>"{@code foo-bar}" = "{@code foo-bar}"</li>
		 * <li>"{@code fooBar}" = "{@code foobar}"</li>
		 * <li>"{@code foo_bar}" = "{@code foobar}"</li>
		 * <li>"{@code [Foo.bar]}" = "{@code Foo.bar}"</li>
		 * </ul>
		 */
		CONFIGURATION {

			@Override
			protected boolean isIncluded(char ch) {
				return Character.isAlphabetic(ch) || Character.isDigit(ch) || (ch == '-');
			}

		},

		/**
		 * The uniform configuration form (used for equals/hashcode; lower-case with only
		 * alphanumeric characters).
		 * <ul>
		 * <li>"{@code foo-bar}" = "{@code foobar}"</li>
		 * <li>"{@code fooBar}" = "{@code foobar}"</li>
		 * <li>"{@code foo_bar}" = "{@code foobar}"</li>
		 * <li>"{@code [Foo.bar]}" = "{@code Foo.bar}"</li>
		 * </ul>
		 */
		UNIFORM {

			@Override
			protected boolean isIncluded(char ch) {
				return Character.isAlphabetic(ch) || Character.isDigit(ch);
			}

		};

		/**
		 * Called to convert an original value into the instance form.
		 * @param value the value to convert
		 * @return the converted value
		 */
		protected String convert(String value) {
			StringBuilder result = new StringBuilder(value.length());
			for (int i = 0; i < value.length(); i++) {
				char ch = value.charAt(i);
				if (isIncluded(ch)) {
					result.append(Character.toLowerCase(ch));
				}
			}
			return result.toString();
		}

		/**
		 * Called to determine of the specified character is valid for the form.
		 * @param ch the character to test
		 * @return if the character is value
		 */
		protected boolean isIncluded(char ch) {
			return true;
		}

		/**
		 * Expand the given value to all an array containing the value in each
		 * {@link Form}.
		 * @param value the source value
		 * @param indexed if the value is indexed
		 * @return an array of all forms (in the same order as {@link Form#values()}.
		 */
		protected static String[] expand(String value, boolean indexed) {
			String[] result = new String[values().length];
			for (Form form : values()) {
				result[form.ordinal()] = (indexed ? value : form.convert(value));
			}
			return result;
		}

	}

}
