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
 * Tests for {@link ConfigurationTable}.
 *
 * @author Brian Clozel
 */
class ConfigurationTableTests {

	private static final String NEWLINE = System.lineSeparator();

	@Test
	void simpleTable() {
		ConfigurationTable table = new ConfigurationTable("test");
		ConfigurationProperty first = new ConfigurationProperty("spring.test.prop", "java.lang.String", "something",
				"This is a description.", false);
		ConfigurationProperty second = new ConfigurationProperty("spring.test.other", "java.lang.String", "other value",
				"This is another description.", false);
		table.addEntry(new SingleConfigurationTableEntry(first));
		table.addEntry(new SingleConfigurationTableEntry(second));
		assertThat(table.toAsciidocTable()).isEqualTo("[cols=\"2,1,1\", options=\"header\"]" + NEWLINE + "|==="
				+ NEWLINE + "|Key|Default Value|Description" + NEWLINE + NEWLINE
				+ "|[[spring.test.other]]<<spring.test.other,`+spring.test.other+`>>" + NEWLINE + "|`+other value+`"
				+ NEWLINE + "|+++This is another description.+++" + NEWLINE + NEWLINE
				+ "|[[spring.test.prop]]<<spring.test.prop,`+spring.test.prop+`>>" + NEWLINE + "|`+something+`"
				+ NEWLINE + "|+++This is a description.+++" + NEWLINE + NEWLINE + "|===" + NEWLINE);
	}

}
