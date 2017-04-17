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

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.boot.context.properties.source.ConfigurationPropertyName.Element;
import org.springframework.boot.context.properties.source.ConfigurationPropertyNameBuilder.ElementValueProcessor;
import org.springframework.test.util.ReflectionTestUtils;

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
	public void createWhenPatternIsNullShouldThrowException() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Pattern must not be null");
		this.builder = new ConfigurationPropertyNameBuilder((Pattern) null);
	}

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
		ConfigurationPropertyName name = this.builder.from("foo.bar.baz", '.').build();
		assertThat(name.toString()).isEqualTo(expected.toString());
	}

	@Test
	public void buildShouldValidateUsingPattern() {
		Pattern pattern = Pattern.compile("[a-z]([a-z0-9\\-])*");
		this.builder = new ConfigurationPropertyNameBuilder(pattern);
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Element value 'foo@!' is not valid");
		this.builder.from("foo@!.bar", '.').build();
	}

	@Test
	public void buildWhenHasNoElementsShouldThrowException() throws Exception {
		this.builder = new ConfigurationPropertyNameBuilder();
		this.thrown.expect(IllegalStateException.class);
		this.thrown.expectMessage("At least one element must be defined");
		this.builder.build();
	}

	@Test
	public void buildShouldUseElementProcessor() throws Exception {
		this.builder = new ConfigurationPropertyNameBuilder(
				value -> value.replace("-", ""));
		ConfigurationPropertyName name = this.builder.from("FOO_THE-BAR", '_').build();
		assertThat(name.toString()).isEqualTo("foo.thebar");
	}

	@Test
	public void fromNameShouldSetElements() throws Exception {
		this.builder = new ConfigurationPropertyNameBuilder();
		ConfigurationPropertyName name = this.builder.from("foo.bar", '.').build();
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
	public void fromNameWhenHasExistingShouldSetNewElements() throws Exception {
		this.thrown.expect(IllegalStateException.class);
		this.thrown.expectMessage("Existing elements must not be present");
		new ConfigurationPropertyNameBuilder().from("foo.bar", '.').from("baz", '.')
				.build();
	}

	@Test
	public void appendShouldAppendElement() throws Exception {
		this.builder = new ConfigurationPropertyNameBuilder();
		ConfigurationPropertyName name = this.builder.from("foo.bar", '.').append("baz")
				.build();
		assertThat(name.toString()).isEqualTo("foo.bar.baz");
	}

	private List<Element> elements(String... elements) {
		return Arrays.stream(elements).map(Element::new).collect(Collectors.toList());
	}

	@SuppressWarnings("unchecked")
	private List<Element> getElements(String name) {
		ConfigurationPropertyNameBuilder builder = this.builder.from(name, '.');
		return (List<Element>) ReflectionTestUtils.getField(builder, "elements");
	}

}
