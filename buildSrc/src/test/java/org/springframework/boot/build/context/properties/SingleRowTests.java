/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.build.context.properties;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SingleRow}.
 *
 * @author Brian Clozel
 * @author Moritz Halbritter
 */
class SingleRowTests {

	private static final String NEWLINE = System.lineSeparator();

	private static final Snippet SNIPPET = new Snippet("my", "title", null);

	@Test
	void simpleProperty() {
		ConfigurationProperty property = new ConfigurationProperty("spring.test.prop", "java.lang.String", "something",
				"This is a description.", false, null);
		SingleRow row = new SingleRow(SNIPPET, property);
		Asciidoc asciidoc = new Asciidoc();
		row.write(asciidoc);
		assertThat(asciidoc).hasToString("|[[my.spring.test.prop]]xref:#my.spring.test.prop[`+spring.test.prop+`]"
				+ NEWLINE + "|+++This is a description.+++" + NEWLINE + "|`+something+`" + NEWLINE);
	}

	@Test
	void noDefaultValue() {
		ConfigurationProperty property = new ConfigurationProperty("spring.test.prop", "java.lang.String", null,
				"This is a description.", false, null);
		SingleRow row = new SingleRow(SNIPPET, property);
		Asciidoc asciidoc = new Asciidoc();
		row.write(asciidoc);
		assertThat(asciidoc).hasToString("|[[my.spring.test.prop]]xref:#my.spring.test.prop[`+spring.test.prop+`]"
				+ NEWLINE + "|+++This is a description.+++" + NEWLINE + "|" + NEWLINE);
	}

	@Test
	void defaultValueWithPipes() {
		ConfigurationProperty property = new ConfigurationProperty("spring.test.prop", "java.lang.String",
				"first|second", "This is a description.", false, null);
		SingleRow row = new SingleRow(SNIPPET, property);
		Asciidoc asciidoc = new Asciidoc();
		row.write(asciidoc);
		assertThat(asciidoc).hasToString("|[[my.spring.test.prop]]xref:#my.spring.test.prop[`+spring.test.prop+`]"
				+ NEWLINE + "|+++This is a description.+++" + NEWLINE + "|`+first\\|second+`" + NEWLINE);
	}

	@Test
	void defaultValueWithBackslash() {
		ConfigurationProperty property = new ConfigurationProperty("spring.test.prop", "java.lang.String",
				"first\\second", "This is a description.", false, null);
		SingleRow row = new SingleRow(SNIPPET, property);
		Asciidoc asciidoc = new Asciidoc();
		row.write(asciidoc);
		assertThat(asciidoc).hasToString("|[[my.spring.test.prop]]xref:#my.spring.test.prop[`+spring.test.prop+`]"
				+ NEWLINE + "|+++This is a description.+++" + NEWLINE + "|`+first\\\\second+`" + NEWLINE);
	}

	@Test
	void descriptionWithPipe() {
		ConfigurationProperty property = new ConfigurationProperty("spring.test.prop", "java.lang.String", null,
				"This is a description with a | pipe.", false, null);
		SingleRow row = new SingleRow(SNIPPET, property);
		Asciidoc asciidoc = new Asciidoc();
		row.write(asciidoc);
		assertThat(asciidoc).hasToString("|[[my.spring.test.prop]]xref:#my.spring.test.prop[`+spring.test.prop+`]"
				+ NEWLINE + "|+++This is a description with a \\| pipe.+++" + NEWLINE + "|" + NEWLINE);
	}

	@Test
	void mapProperty() {
		ConfigurationProperty property = new ConfigurationProperty("spring.test.prop",
				"java.util.Map<java.lang.String,java.lang.String>", null, "This is a description.", false, null);
		SingleRow row = new SingleRow(SNIPPET, property);
		Asciidoc asciidoc = new Asciidoc();
		row.write(asciidoc);
		assertThat(asciidoc).hasToString("|[[my.spring.test.prop]]xref:#my.spring.test.prop[`+spring.test.prop.*+`]"
				+ NEWLINE + "|+++This is a description.+++" + NEWLINE + "|" + NEWLINE);
	}

	@Test
	void listProperty() {
		String[] defaultValue = new String[] { "first", "second", "third" };
		ConfigurationProperty property = new ConfigurationProperty("spring.test.prop",
				"java.util.List<java.lang.String>", defaultValue, "This is a description.", false, null);
		SingleRow row = new SingleRow(SNIPPET, property);
		Asciidoc asciidoc = new Asciidoc();
		row.write(asciidoc);
		assertThat(asciidoc).hasToString("|[[my.spring.test.prop]]xref:#my.spring.test.prop[`+spring.test.prop+`]"
				+ NEWLINE + "|+++This is a description.+++" + NEWLINE + "|`+first," + NEWLINE + "second," + NEWLINE
				+ "third+`" + NEWLINE);
	}

}
