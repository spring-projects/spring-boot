/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.build.context.properties;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SingleConfigurationTableEntry}.
 *
 * @author Brian Clozel
 */
public class SingleConfigurationTableEntryTests {

	private static String NEWLINE = System.lineSeparator();

	@Test
	void simpleProperty() {
		ConfigurationProperty property = new ConfigurationProperty("spring.test.prop", "java.lang.String", "something",
				"This is a description.", false);
		SingleConfigurationTableEntry entry = new SingleConfigurationTableEntry(property);
		AsciidocBuilder builder = new AsciidocBuilder();
		entry.write(builder);
		assertThat(builder.toString()).isEqualTo("|`+spring.test.prop+`" + NEWLINE + "|`+something+`" + NEWLINE
				+ "|+++This is a description.+++" + NEWLINE);
	}

	@Test
	void noDefaultValue() {
		ConfigurationProperty property = new ConfigurationProperty("spring.test.prop", "java.lang.String", null,
				"This is a description.", false);
		SingleConfigurationTableEntry entry = new SingleConfigurationTableEntry(property);
		AsciidocBuilder builder = new AsciidocBuilder();
		entry.write(builder);
		assertThat(builder.toString()).isEqualTo(
				"|`+spring.test.prop+`" + NEWLINE + "|" + NEWLINE + "|+++This is a description.+++" + NEWLINE);
	}

	@Test
	void defaultValueWithPipes() {
		ConfigurationProperty property = new ConfigurationProperty("spring.test.prop", "java.lang.String",
				"first|second", "This is a description.", false);
		SingleConfigurationTableEntry entry = new SingleConfigurationTableEntry(property);
		AsciidocBuilder builder = new AsciidocBuilder();
		entry.write(builder);
		assertThat(builder.toString()).isEqualTo("|`+spring.test.prop+`" + NEWLINE + "|`+first\\|second+`" + NEWLINE
				+ "|+++This is a description.+++" + NEWLINE);
	}

	@Test
	void defaultValueWithBackslash() {
		ConfigurationProperty property = new ConfigurationProperty("spring.test.prop", "java.lang.String",
				"first\\second", "This is a description.", false);
		SingleConfigurationTableEntry entry = new SingleConfigurationTableEntry(property);
		AsciidocBuilder builder = new AsciidocBuilder();
		entry.write(builder);
		assertThat(builder.toString()).isEqualTo("|`+spring.test.prop+`" + NEWLINE + "|`+first\\\\second+`" + NEWLINE
				+ "|+++This is a description.+++" + NEWLINE);
	}

	@Test
	void descriptionWithPipe() {
		ConfigurationProperty property = new ConfigurationProperty("spring.test.prop", "java.lang.String", null,
				"This is a description with a | pipe.", false);
		SingleConfigurationTableEntry entry = new SingleConfigurationTableEntry(property);
		AsciidocBuilder builder = new AsciidocBuilder();
		entry.write(builder);
		assertThat(builder.toString()).isEqualTo("|`+spring.test.prop+`" + NEWLINE + "|" + NEWLINE
				+ "|+++This is a description with a \\| pipe.+++" + NEWLINE);
	}

	@Test
	void mapProperty() {
		ConfigurationProperty property = new ConfigurationProperty("spring.test.prop",
				"java.util.Map<java.lang.String,java.lang.String>", null, "This is a description.", false);
		SingleConfigurationTableEntry entry = new SingleConfigurationTableEntry(property);
		AsciidocBuilder builder = new AsciidocBuilder();
		entry.write(builder);
		assertThat(builder.toString()).isEqualTo(
				"|`+spring.test.prop.*+`" + NEWLINE + "|" + NEWLINE + "|+++This is a description.+++" + NEWLINE);
	}

	@Test
	void listProperty() {
		String[] defaultValue = new String[] { "first", "second", "third" };
		ConfigurationProperty property = new ConfigurationProperty("spring.test.prop",
				"java.util.List<java.lang.String>", defaultValue, "This is a description.", false);
		SingleConfigurationTableEntry entry = new SingleConfigurationTableEntry(property);
		AsciidocBuilder builder = new AsciidocBuilder();
		entry.write(builder);
		assertThat(builder.toString()).isEqualTo("|`+spring.test.prop+`" + NEWLINE + "|`+first," + NEWLINE + "second,"
				+ NEWLINE + "third+`" + NEWLINE + "|+++This is a description.+++" + NEWLINE);
	}

}
