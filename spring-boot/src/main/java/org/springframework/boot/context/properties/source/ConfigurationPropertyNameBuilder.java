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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.boot.context.properties.source.ConfigurationPropertyName.Element;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName.Form;
import org.springframework.util.Assert;
import org.springframework.util.ConcurrentReferenceHashMap;

/**
 * Builder class that can be used to create {@link ConfigurationPropertyName
 * ConfigurationPropertyNames}. This class is intended for use within custom
 * {@link ConfigurationPropertySource} implementations. When accessing
 * {@link ConfigurationProperty properties} from and existing
 * {@link ConfigurationPropertySource source} the
 * {@link ConfigurationPropertyName#of(String)} method should be used to obtain a
 * {@link ConfigurationPropertyName name}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @see ConfigurationPropertyName
 */
class ConfigurationPropertyNameBuilder {

	private static final Element INDEX_ZERO_ELEMENT = new Element("[0]");

	private final ElementValueProcessor processor;

	private final Map<String, ConfigurationPropertyName> nameCache = new ConcurrentReferenceHashMap<>();

	ConfigurationPropertyNameBuilder() {
		this(ElementValueProcessor.identity());
	}

	ConfigurationPropertyNameBuilder(ElementValueProcessor processor) {
		Assert.notNull(processor, "Processor must not be null");
		this.processor = processor;
	}

	/**
	 * Build using the specified name split up into elements using a known separator. For
	 * example {@code from("foo.bar", '.')} will return a new builder containing the
	 * elements "{@code foo}" and "{@code bar}". Any element in square brackets will be
	 * considered "indexed" and will not be considered for splitting.
	 * @param name the name build from
	 * @param separator the separator
	 * @return a builder with elements populated from the name
	 */
	public ConfigurationPropertyName from(String name, char separator) {
		Assert.notNull(name, "Name must not be null");
		ConfigurationPropertyName result = this.nameCache.get(name);
		if (result != null) {
			return result;
		}
		List<Element> elements = new ArrayList<>();
		StringBuilder value = new StringBuilder(name.length());
		boolean indexed = false;
		for (int i = 0; i < name.length(); i++) {
			char ch = name.charAt(i);
			if (!indexed) {
				if (ch == '[') {
					addElement(elements, value);
					value.append(ch);
					indexed = true;
				}
				else if (ch == separator) {
					addElement(elements, value);
				}
				else {
					value.append(ch);
				}
			}
			else {
				value.append(ch);
				if (ch == ']') {
					addElement(elements, value);
					indexed = false;
				}
			}
		}
		addElement(elements, value);
		result = from(elements.stream().filter(Objects::nonNull)
				.filter((e) -> !e.getValue(Form.UNIFORM).isEmpty()).iterator());
		this.nameCache.put(name, result);
		return result;
	}

	private void addElement(List<Element> elements, StringBuilder value) {
		if (value.length() > 0) {
			elements.add(buildElement(value.toString()));
			value.setLength(0);
		}
	}

	private ConfigurationPropertyName from(Iterator<Element> elements) {
		ConfigurationPropertyName name = null;
		while (elements.hasNext()) {
			name = new ConfigurationPropertyName(name, elements.next());
		}
		Assert.state(name != null, "At least one element must be defined");
		return name;
	}

	public ConfigurationPropertyName from(ConfigurationPropertyName parent, int index) {
		if (index == 0) {
			return new ConfigurationPropertyName(parent, INDEX_ZERO_ELEMENT);
		}
		return from(parent, "[" + index + "]");

	}

	public ConfigurationPropertyName from(ConfigurationPropertyName parent,
			String elementValue) {
		return new ConfigurationPropertyName(parent, buildElement(elementValue));
	}

	private Element buildElement(String value) {
		return new Element(this.processor.apply(value));
	}

	/**
	 * An processor that will be applied to element values. Can be used to manipulate or
	 * restrict the values that are used.
	 */
	@FunctionalInterface
	public interface ElementValueProcessor {

		/**
		 * Apply the processor to the specified value.
		 * @param value the value to process
		 * @return the processed value
		 * @throws RuntimeException if the value cannot be used
		 */
		String apply(String value) throws RuntimeException;

		/**
		 * Return an empty {@link ElementValueProcessor} that simply returns the original
		 * value unchanged.
		 * @return an empty {@link ElementValueProcessor}.
		 */
		static ElementValueProcessor identity() {
			return (value) -> value;
		}

		/**
		 * Extend this processor with a to enforce standard element name rules.
		 * @return an element processor that additionally enforces a valid name
		 */
		default ElementValueProcessor withValidName() {
			return (value) -> {
				value = apply(value);
				if (!Element.isIndexed(value)) {
					for (int i = 0; i < value.length(); i++) {
						char ch = value.charAt(i);
						boolean isAlpha = ch >= 'a' && ch <= 'z';
						boolean isNumeric = ch >= '0' && ch <= '9';
						if (i == 0 && !isAlpha || !(isAlpha || isNumeric || ch == '-')) {
							throw new IllegalArgumentException(
									"Element value '" + value + "' is not valid");
						}
					}
				}
				return value;
			};
		}

	}

}
