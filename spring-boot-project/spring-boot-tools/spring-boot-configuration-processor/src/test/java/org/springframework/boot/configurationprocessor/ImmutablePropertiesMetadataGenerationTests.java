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
import org.springframework.boot.configurationprocessor.metadata.Metadata;
import org.springframework.boot.configurationsample.immutable.ImmutableSimpleProperties;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Metadata generation tests for immutable properties.
 *
 * @author Stephane Nicoll
 */
class ImmutablePropertiesMetadataGenerationTests extends AbstractMetadataGenerationTests {

	@Test
	void immutableSimpleProperties() {
		ConfigurationMetadata metadata = compile(ImmutableSimpleProperties.class);
		assertThat(metadata).has(Metadata.withGroup("immutable").fromSource(ImmutableSimpleProperties.class));
		assertThat(metadata).has(
				Metadata.withProperty("immutable.the-name", String.class).fromSource(ImmutableSimpleProperties.class)
						.withDescription("The name of this simple properties.").withDefaultValue("boot"));
		assertThat(metadata).has(Metadata.withProperty("immutable.flag", Boolean.class).withDefaultValue(false)
				.fromSource(ImmutableSimpleProperties.class).withDescription("A simple flag.")
				.withDeprecation(null, null));
		assertThat(metadata).has(Metadata.withProperty("immutable.comparator"));
		assertThat(metadata).has(Metadata.withProperty("immutable.counter"));
		assertThat(metadata.getItems()).hasSize(5);
	}

}
