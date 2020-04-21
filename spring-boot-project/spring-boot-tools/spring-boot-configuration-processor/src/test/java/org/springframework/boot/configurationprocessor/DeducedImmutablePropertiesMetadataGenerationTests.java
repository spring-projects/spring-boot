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

package org.springframework.boot.configurationprocessor;

import org.junit.jupiter.api.Test;

import org.springframework.boot.configurationprocessor.metadata.ConfigurationMetadata;
import org.springframework.boot.configurationprocessor.metadata.ItemMetadata;
import org.springframework.boot.configurationprocessor.metadata.Metadata;
import org.springframework.boot.configurationsample.immutable.DeducedImmutableClassProperties;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Metadata generation tests for immutable properties deduced because they're nested.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 */
class DeducedImmutablePropertiesMetadataGenerationTests extends AbstractMetadataGenerationTests {

	@Test
	void immutableSimpleProperties() {
		ConfigurationMetadata metadata = compile(DeducedImmutableClassProperties.class);
		assertThat(metadata).has(Metadata.withGroup("test").fromSource(DeducedImmutableClassProperties.class));
		assertThat(metadata).has(Metadata.withGroup("test.nested", DeducedImmutableClassProperties.Nested.class)
				.fromSource(DeducedImmutableClassProperties.class));
		assertThat(metadata).has(Metadata.withProperty("test.nested.name", String.class)
				.fromSource(DeducedImmutableClassProperties.Nested.class));
		ItemMetadata nestedMetadata = metadata.getItems().stream()
				.filter((item) -> item.getName().equals("test.nested")).findFirst().get();
		assertThat(nestedMetadata.getDefaultValue()).isNull();
	}

}
