/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.configurationdocs;

import org.junit.jupiter.api.Test;

import org.springframework.boot.configurationmetadata.ConfigurationMetadataProperty;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SingleConfigurationTableEntry}.
 *
 * @author Brian Clozel
 */
class SingleConfigurationTableEntryTests {

	private static String NEWLINE = System.lineSeparator();

	@Test
	void simpleProperty() {
		ConfigurationMetadataProperty property = new ConfigurationMetadataProperty();
		property.setId("spring.test.prop");
		property.setDefaultValue("something");
		property.setDescription("This is a description.");
		property.setType("java.lang.String");
		SingleConfigurationTableEntry entry = new SingleConfigurationTableEntry(property);
		AsciidocBuilder builder = new AsciidocBuilder();
		entry.write(builder);
		assertThat(builder.toString()).isEqualTo("|`+spring.test.prop+`" + NEWLINE + "|`+something+`" + NEWLINE
				+ "|+++This is a description.+++" + NEWLINE);
	}

	@Test
	void noDefaultValue() {
		ConfigurationMetadataProperty property = new ConfigurationMetadataProperty();
		property.setId("spring.test.prop");
		property.setDescription("This is a description.");
		property.setType("java.lang.String");
		SingleConfigurationTableEntry entry = new SingleConfigurationTableEntry(property);
		AsciidocBuilder builder = new AsciidocBuilder();
		entry.write(builder);
		assertThat(builder.toString()).isEqualTo(
				"|`+spring.test.prop+`" + NEWLINE + "|" + NEWLINE + "|+++This is a description.+++" + NEWLINE);
	}

	@Test
	void defaultValueWithPipes() {
		ConfigurationMetadataProperty property = new ConfigurationMetadataProperty();
		property.setId("spring.test.prop");
		property.setDefaultValue("first|second");
		property.setDescription("This is a description.");
		property.setType("java.lang.String");
		SingleConfigurationTableEntry entry = new SingleConfigurationTableEntry(property);
		AsciidocBuilder builder = new AsciidocBuilder();
		entry.write(builder);
		assertThat(builder.toString()).isEqualTo("|`+spring.test.prop+`" + NEWLINE + "|`+first{vbar}" + NEWLINE
				+ "second+`" + NEWLINE + "|+++This is a description.+++" + NEWLINE);
	}

	@Test
	void defaultValueWithBackslash() {
		ConfigurationMetadataProperty property = new ConfigurationMetadataProperty();
		property.setId("spring.test.prop");
		property.setDefaultValue("first\\second");
		property.setDescription("This is a description.");
		property.setType("java.lang.String");
		SingleConfigurationTableEntry entry = new SingleConfigurationTableEntry(property);
		AsciidocBuilder builder = new AsciidocBuilder();
		entry.write(builder);
		assertThat(builder.toString()).isEqualTo("|`+spring.test.prop+`" + NEWLINE + "|`+first\\\\second+`" + NEWLINE
				+ "|+++This is a description.+++" + NEWLINE);
	}

	@Test
	void mapProperty() {
		ConfigurationMetadataProperty property = new ConfigurationMetadataProperty();
		property.setId("spring.test.prop");
		property.setDescription("This is a description.");
		property.setType("java.util.Map<java.lang.String,java.lang.String>");
		SingleConfigurationTableEntry entry = new SingleConfigurationTableEntry(property);
		AsciidocBuilder builder = new AsciidocBuilder();
		entry.write(builder);
		assertThat(builder.toString()).isEqualTo(
				"|`+spring.test.prop.*+`" + NEWLINE + "|" + NEWLINE + "|+++This is a description.+++" + NEWLINE);
	}

	@Test
	void listProperty() {
		String[] defaultValue = new String[] { "first", "second", "third" };
		ConfigurationMetadataProperty property = new ConfigurationMetadataProperty();
		property.setId("spring.test.prop");
		property.setDescription("This is a description.");
		property.setType("java.util.List<java.lang.String>");
		property.setDefaultValue(defaultValue);
		SingleConfigurationTableEntry entry = new SingleConfigurationTableEntry(property);
		AsciidocBuilder builder = new AsciidocBuilder();
		entry.write(builder);
		assertThat(builder.toString()).isEqualTo("|`+spring.test.prop+`" + NEWLINE + "|`+first," + NEWLINE + "second,"
				+ NEWLINE + "third+`" + NEWLINE + "|+++This is a description.+++" + NEWLINE);
	}

}
