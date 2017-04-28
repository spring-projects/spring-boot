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
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.boot.context.properties.source.ConfigurationPropertyName.Element;
import org.springframework.boot.context.properties.source.ConfigurationPropertyNameBuilder.ElementValueProcessor;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ConfigurationPropertyNameBuilder}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
public class ConfigurationPropertyNameBuilderTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private ConfigurationPropertyNameBuilder builder;

	@Test
	public void createWhenElementProcessorIsNullShouldThrowException() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Processor must not be null");
		this.builder = new ConfigurationPropertyNameBuilder((ElementValueProcessor) null);
	}

	@Test
	public void buildShouldCreateName() throws Exception {
		this.builder = new ConfigurationPropertyNameBuilder();
		ConfigurationPropertyName expected = ConfigurationPropertyName.of("foo.bar.baz");
		ConfigurationPropertyName name = this.builder.from("foo.bar.baz", '.');
		assertThat(name.toString()).isEqualTo(expected.toString());
	}

	@Test
	public void buildShouldValidateProcessor() {
		this.builder = new ConfigurationPropertyNameBuilder(
				ElementValueProcessor.identity().withValidName());
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Element value 'foo@!' is not valid");
		this.builder.from("foo@!.bar", '.');
	}

	@Test
	public void buildShouldUseElementProcessor() throws Exception {
		this.builder = new ConfigurationPropertyNameBuilder(
				value -> value.replace("-", ""));
		ConfigurationPropertyName name = this.builder.from("FOO_THE-BAR", '_');
		assertThat(name.toString()).isEqualTo("foo.thebar");
	}

	@Test
	public void fromNameShouldSetElements() throws Exception {
		this.builder = new ConfigurationPropertyNameBuilder();
		ConfigurationPropertyName name = this.builder.from("foo.bar", '.');
		assertThat(name.toString()).isEqualTo("foo.bar");
	}

	@Test
	public void fromNameShouldSetIndexedElements() throws Exception {
		this.builder = new ConfigurationPropertyNameBuilder();
		assertThat(getElements("foo")).isEqualTo(elements("foo"));
		assertThat(getElements("[foo]")).isEqualTo(elements("[foo]"));
		assertThat(getElements("foo.bar")).isEqualTo(elements("foo", "bar"));
		assertThat(getElements("foo[foo.bar]")).isEqualTo(elements("foo", "[foo.bar]"));
		assertThat(getElements("foo.[bar].baz"))
				.isEqualTo(elements("foo", "[bar]", "baz"));
	}

	@Test
	public void appendShouldAppendElement() throws Exception {
		this.builder = new ConfigurationPropertyNameBuilder();
		ConfigurationPropertyName parent = this.builder.from("foo.bar", '.');
		ConfigurationPropertyName name = this.builder.from(parent, "baz");
		assertThat(name.toString()).isEqualTo("foo.bar.baz");
	}

	private List<Element> elements(String... elements) {
		return Arrays.stream(elements).map(Element::new).collect(Collectors.toList());
	}

	private List<Element> getElements(String name) {
		List<Element> elements = new ArrayList<>();
		for (Element element : this.builder.from(name, '.')) {
			elements.add(element);
		}
		return elements;
	}

}
