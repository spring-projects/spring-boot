/*
 * Copyright 2012-2021 the original author or authors.
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
 * Tests for {@link SingleRow}.
 *
 * @author Brian Clozel
 */
public class SingleRowTests {

	private static final String NEWLINE = System.lineSeparator();

	private static final Snippet SNIPPET = new Snippet("my", "title", null);

	@Test
	void simpleProperty() {
		ConfigurationProperty property = new ConfigurationProperty("spring.test.prop", "java.lang.String", "something",
				"This is a description.", false);
		SingleRow row = new SingleRow(SNIPPET, property);
		Asciidoc asciidoc = new Asciidoc();
		row.write(asciidoc);
		assertThat(asciidoc.toString()).isEqualTo("|[[my.spring.test.prop]]<<my.spring.test.prop,`+spring.test.prop+`>>"
				+ NEWLINE + "|+++This is a description.+++" + NEWLINE + "|`+something+`" + NEWLINE);
	}

	@Test
	void noDefaultValue() {
		ConfigurationProperty property = new ConfigurationProperty("spring.test.prop", "java.lang.String", null,
				"This is a description.", false);
		SingleRow row = new SingleRow(SNIPPET, property);
		Asciidoc asciidoc = new Asciidoc();
		row.write(asciidoc);
		assertThat(asciidoc.toString()).isEqualTo("|[[my.spring.test.prop]]<<my.spring.test.prop,`+spring.test.prop+`>>"
				+ NEWLINE + "|+++This is a description.+++" + NEWLINE + "|" + NEWLINE);
	}

	@Test
	void defaultValueWithPipes() {
		ConfigurationProperty property = new ConfigurationProperty("spring.test.prop", "java.lang.String",
				"first|second", "This is a description.", false);
		SingleRow row = new SingleRow(SNIPPET, property);
		Asciidoc asciidoc = new Asciidoc();
		row.write(asciidoc);
		assertThat(asciidoc.toString()).isEqualTo("|[[my.spring.test.prop]]<<my.spring.test.prop,`+spring.test.prop+`>>"
				+ NEWLINE + "|+++This is a description.+++" + NEWLINE + "|`+first\\|second+`" + NEWLINE);
	}

	@Test
	void defaultValueWithBackslash() {
		ConfigurationProperty property = new ConfigurationProperty("spring.test.prop", "java.lang.String",
				"first\\second", "This is a description.", false);
		SingleRow row = new SingleRow(SNIPPET, property);
		Asciidoc asciidoc = new Asciidoc();
		row.write(asciidoc);
		assertThat(asciidoc.toString()).isEqualTo("|[[my.spring.test.prop]]<<my.spring.test.prop,`+spring.test.prop+`>>"
				+ NEWLINE + "|+++This is a description.+++" + NEWLINE + "|`+first\\\\second+`" + NEWLINE);
	}

	@Test
	void descriptionWithPipe() {
		ConfigurationProperty property = new ConfigurationProperty("spring.test.prop", "java.lang.String", null,
				"This is a description with a | pipe.", false);
		SingleRow row = new SingleRow(SNIPPET, property);
		Asciidoc asciidoc = new Asciidoc();
		row.write(asciidoc);
		assertThat(asciidoc.toString()).isEqualTo("|[[my.spring.test.prop]]<<my.spring.test.prop,`+spring.test.prop+`>>"
				+ NEWLINE + "|+++This is a description with a \\| pipe.+++" + NEWLINE + "|" + NEWLINE);
	}

	@Test
	void mapProperty() {
		ConfigurationProperty property = new ConfigurationProperty("spring.test.prop",
				"java.util.Map<java.lang.String,java.lang.String>", null, "This is a description.", false);
		SingleRow row = new SingleRow(SNIPPET, property);
		Asciidoc asciidoc = new Asciidoc();
		row.write(asciidoc);
		assertThat(asciidoc.toString())
				.isEqualTo("|[[my.spring.test.prop]]<<my.spring.test.prop,`+spring.test.prop.*+`>>" + NEWLINE
						+ "|+++This is a description.+++" + NEWLINE + "|" + NEWLINE);
	}

	@Test
	void listProperty() {
		String[] defaultValue = new String[] { "first", "second", "third" };
		ConfigurationProperty property = new ConfigurationProperty("spring.test.prop",
				"java.util.List<java.lang.String>", defaultValue, "This is a description.", false);
		SingleRow row = new SingleRow(SNIPPET, property);
		Asciidoc asciidoc = new Asciidoc();
		row.write(asciidoc);
		assertThat(asciidoc.toString()).isEqualTo("|[[my.spring.test.prop]]<<my.spring.test.prop,`+spring.test.prop+`>>"
				+ NEWLINE + "|+++This is a description.+++" + NEWLINE + "|`+first," + NEWLINE + "second," + NEWLINE
				+ "third+`" + NEWLINE);
	}

}
