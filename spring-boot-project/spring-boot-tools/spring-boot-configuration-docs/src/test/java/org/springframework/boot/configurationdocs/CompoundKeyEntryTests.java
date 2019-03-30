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

import org.junit.Test;

import org.springframework.boot.configurationmetadata.ConfigurationMetadataProperty;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Brian Clozel
 */
public class CompoundKeyEntryTests {

	private static String NEWLINE = System.lineSeparator();

	@Test
	public void simpleProperty() {
		ConfigurationMetadataProperty firstProp = new ConfigurationMetadataProperty();
		firstProp.setId("spring.test.first");
		firstProp.setType("java.lang.String");

		ConfigurationMetadataProperty secondProp = new ConfigurationMetadataProperty();
		secondProp.setId("spring.test.second");
		secondProp.setType("java.lang.String");

		ConfigurationMetadataProperty thirdProp = new ConfigurationMetadataProperty();
		thirdProp.setId("spring.test.third");
		thirdProp.setType("java.lang.String");

		CompoundKeyEntry entry = new CompoundKeyEntry("spring.test",
				"This is a description.");
		entry.addConfigurationKeys(firstProp, secondProp, thirdProp);
		StringBuilder builder = new StringBuilder();
		entry.writeAsciidoc(builder);

		assertThat(builder.toString()).isEqualTo("|`+++spring.test.first" + NEWLINE
				+ "spring.test.second" + NEWLINE + "spring.test.third" + NEWLINE + "+++`"
				+ NEWLINE + "|" + NEWLINE + "|+++This is a description.+++" + NEWLINE);
	}

}
