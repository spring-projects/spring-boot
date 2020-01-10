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
 * Tests for {@link CompoundConfigurationTableEntry}.
 *
 * @author Brian Clozel
 */
public class CompoundConfigurationTableEntryTests {

	private static String NEWLINE = System.lineSeparator();

	@Test
	void simpleProperty() {
		ConfigurationProperty firstProp = new ConfigurationProperty("spring.test.first", "java.lang.String");
		ConfigurationProperty secondProp = new ConfigurationProperty("spring.test.second", "java.lang.String");
		ConfigurationProperty thirdProp = new ConfigurationProperty("spring.test.third", "java.lang.String");
		CompoundConfigurationTableEntry entry = new CompoundConfigurationTableEntry("spring.test",
				"This is a description.");
		entry.addConfigurationKeys(firstProp, secondProp, thirdProp);
		AsciidocBuilder builder = new AsciidocBuilder();
		entry.write(builder);
		assertThat(builder.toString()).isEqualTo(
				"|`+spring.test.first+` +" + NEWLINE + "`+spring.test.second+` +" + NEWLINE + "`+spring.test.third+` +"
						+ NEWLINE + NEWLINE + "|" + NEWLINE + "|+++This is a description.+++" + NEWLINE);
	}

}
