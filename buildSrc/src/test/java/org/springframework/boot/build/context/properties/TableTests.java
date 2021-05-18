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
 * Tests for {@link Table}.
 *
 * @author Brian Clozel
 */
public class TableTests {

	private static final String NEWLINE = System.lineSeparator();

	private static final Snippet SNIPPET = new Snippet("my", "title", null);

	@Test
	void simpleTable() {
		Table table = new Table();
		table.addRow(new SingleRow(SNIPPET, new ConfigurationProperty("spring.test.prop", "java.lang.String",
				"something", "This is a description.", false)));
		table.addRow(new SingleRow(SNIPPET, new ConfigurationProperty("spring.test.other", "java.lang.String",
				"other value", "This is another description.", false)));
		Asciidoc asciidoc = new Asciidoc();
		table.write(asciidoc);
		// @formatter:off
		assertThat(asciidoc.toString()).isEqualTo(
				"[cols=\"4,3,3\", options=\"header\"]" + NEWLINE +
				"|===" + NEWLINE +
				"|Name|Description|Default Value" + NEWLINE + NEWLINE +
				"|[[my.spring.test.other]]<<my.spring.test.other,`+spring.test.other+`>>" + NEWLINE +
				"|+++This is another description.+++" + NEWLINE +
				"|`+other value+`" + NEWLINE + NEWLINE +
				"|[[my.spring.test.prop]]<<my.spring.test.prop,`+spring.test.prop+`>>" + NEWLINE +
				"|+++This is a description.+++" + NEWLINE +
				"|`+something+`" + NEWLINE + NEWLINE +
				"|===" + NEWLINE);
		// @formatter:on
	}

}
