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

package org.springframework.boot.configurationprocessor.metadata;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ItemMetadata}.
 *
 * @author Stephane Nicoll
 */
class ItemMetadataTests {

	@Test
	void newItemMetadataPrefixWithCapitalizedPrefix() {
		assertThat(newItemMetadataPrefix("Prefix.", "value")).isEqualTo("prefix.value");
	}

	@Test
	void newItemMetadataPrefixWithCamelCaseSuffix() {
		assertThat(newItemMetadataPrefix("prefix.", "myValue")).isEqualTo("prefix.my-value");
	}

	@Test
	void newItemMetadataPrefixWithUpperCamelCaseSuffix() {
		assertThat(newItemMetadataPrefix("prefix.", "MyValue")).isEqualTo("prefix.my-value");
	}

	private String newItemMetadataPrefix(String prefix, String suffix) {
		return ItemMetadata.newItemMetadataPrefix(prefix, suffix);
	}

}
