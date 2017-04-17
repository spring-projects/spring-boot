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
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

import org.springframework.boot.context.properties.source.ConfigurationPropertyName.Element;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName.Form;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

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
 * @since 2.0.0
 * @see ConfigurationPropertyName
 */
public class ConfigurationPropertyNameBuilder {

	private final ElementValueProcessor processor;

	private final List<Element> elements;

	/**
	 * Create a new {@link ConfigurationPropertyNameBuilder} instance.
	 */
	public ConfigurationPropertyNameBuilder() {
		this(ElementValueProcessor.empty());
	}

	/**
	 * Create a new {@link ConfigurationPropertyNameBuilder} instance that enforces a
	 * {@link Pattern} on all element values.
	 * @param elementValuePattern the element value pattern to enforce
	 */
	public ConfigurationPropertyNameBuilder(Pattern elementValuePattern) {
		this(ElementValueProcessor.empty().withPatternCheck(elementValuePattern));
	}

	/**
	 * Create a new {@link ConfigurationPropertyNameBuilder} with the specified
	 * {@link ElementValueProcessor}.
	 * @param processor the element value processor.
	 */
	public ConfigurationPropertyNameBuilder(ElementValueProcessor processor) {
		Assert.notNull(processor, "Processor must not be null");
		this.processor = processor;
		this.elements = Collections.emptyList();
	}

	/**
	 * Internal constructor used to create new builders.
	 * @param processor the element value processor.
	 * @param elements the elements built so far
	 */
	private ConfigurationPropertyNameBuilder(ElementValueProcessor processor,
			List<Element> elements) {
		this.processor = processor;
		this.elements = elements;
	}

	/**
	 * Start building using the specified name split up into elements using a known
	 * separator. For example {@code from("foo.bar", '.')} will return a new builder
	 * containing the elements "{@code foo}" and "{@code bar}". Any element in square
	 * brackets will be considered "indexed" and will not be considered for splitting.
	 * @param name the name build from
	 * @param separator the separator
	 * @return a builder with elements populated from the name
	 */
	public ConfigurationPropertyNameBuilder from(String name, char separator) {
		Assert.notNull(name, "Name must not be null");
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
		return from(elements.stream().filter(Objects::nonNull)
				.filter((e) -> !e.getValue(Form.UNIFORM).isEmpty()).iterator());
	}

	private void addElement(List<Element> elements, StringBuilder value) {
		if (value.length() > 0) {
			elements.add(buildElement(value.toString()));
			value.setLength(0);
		}
	}

	/**
	 * Return a new {@link ConfigurationPropertyNameBuilder} starting with the specified
	 * elements.
	 * @param elements the elements that the new builder should contain
	 * @return a new initialized builder instance
	 */
	public ConfigurationPropertyNameBuilder from(Iterable<Element> elements) {
		return from(elements.iterator());
	}

	/**
	 * Return a new {@link ConfigurationPropertyNameBuilder} starting with the specified
	 * elements.
	 * @param elements the elements that the new builder should contain
	 * @return a new initialized builder instance
	 */
	public ConfigurationPropertyNameBuilder from(Iterator<Element> elements) {
		Assert.state(CollectionUtils.isEmpty(this.elements),
				"Existing elements must not be present");
		return new ConfigurationPropertyNameBuilder(this.processor, toList(elements));
	}

	private List<Element> toList(Iterator<Element> iterator) {
		List<Element> list = new ArrayList<>();
		while (iterator.hasNext()) {
			list.add(iterator.next());
		}
		if (isRoot(list)) {
			return Collections.emptyList();
		}
		return list;
	}

	private boolean isRoot(List<Element> list) {
		return (list.size() == 1 && list.get(0).getValue(Form.ORIGINAL).isEmpty());
	}

	/**
	 * Return a new builder containing the elements built so far appended with the
	 * specified element value.
	 * @param elementValue the element value to append
	 * @return a new builder instance
	 */
	public ConfigurationPropertyNameBuilder append(String elementValue) {
		Assert.notNull(elementValue, "ElementValue must not be null");
		List<Element> elements = new ArrayList<>(this.elements);
		elements.add(buildElement(elementValue));
		return new ConfigurationPropertyNameBuilder(this.processor, elements);
	}

	private Element buildElement(String value) {
		return new Element(this.processor.apply(value));
	}

	/**
	 * Build a new {@link ConfigurationPropertyName} from the elements contained in this
	 * builder.
	 * @return a new {@link ConfigurationPropertyName}.
	 */
	public ConfigurationPropertyName build() {
		ConfigurationPropertyName name = null;
		for (Element element : this.elements) {
			name = new ConfigurationPropertyName(name, element);
		}
		Assert.state(name != null, "At least one element must be defined");
		return name;
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
		 * Extend this processor with a {@link Pattern} regular expression check.
		 * @param pattern the patter to check
		 * @return an element processor that additionally checks against the pattern
		 */
		default ElementValueProcessor withPatternCheck(Pattern pattern) {
			Assert.notNull(pattern, "Pattern must not be null");
			return (value) -> {
				value = apply(value);
				Element element = new Element(value);
				Assert.isTrue(element.isIndexed() || pattern.matcher(value).matches(),
						"Element value '" + value + "' is not valid (" + pattern
								+ " does not match)");
				return value;
			};
		}

		/**
		 * Return an empty {@link ElementValueProcessor} that simply returns the original
		 * value unchanged.
		 * @return an empty {@link ElementValueProcessor}.
		 */
		static ElementValueProcessor empty() {
			return (value) -> value;
		}

	}

}
