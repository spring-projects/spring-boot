/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.build.context.properties;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link CompoundRow}.
 *
 * @author Brian Clozel
 * @author Moritz Halbritter
 */
class CompoundRowTests {

	private static final String NEWLINE = System.lineSeparator();

	private static final Snippet SNIPPET = new Snippet("my", "title", null);

	@Test
	void simpleProperty() {
		CompoundRow row = new CompoundRow(SNIPPET, "spring.test", "This is a description.");
		row.addProperty(new ConfigurationProperty("spring.test.first", "java.lang.String"));
		row.addProperty(new ConfigurationProperty("spring.test.second", "java.lang.String"));
		row.addProperty(new ConfigurationProperty("spring.test.third", "java.lang.String"));
		Asciidoc asciidoc = new Asciidoc();
		row.write(asciidoc);
		assertThat(asciidoc).hasToString("|[[my.spring.test]]xref:#my.spring.test[`+spring.test.first+` +" + NEWLINE
				+ "`+spring.test.second+` +" + NEWLINE + "`+spring.test.third+` +" + NEWLINE + "]" + NEWLINE
				+ "|+++This is a description.+++" + NEWLINE + "|" + NEWLINE);
	}

}
