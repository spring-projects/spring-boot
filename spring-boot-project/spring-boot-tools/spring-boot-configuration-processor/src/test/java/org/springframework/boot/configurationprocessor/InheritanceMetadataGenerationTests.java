/*
 * Copyright 2012-2022 the original author or authors.
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
import org.springframework.boot.configurationsample.inheritance.ChildProperties;
import org.springframework.boot.configurationsample.inheritance.ChildPropertiesConfig;
import org.springframework.boot.configurationsample.inheritance.OverrideChildProperties;
import org.springframework.boot.configurationsample.inheritance.OverrideChildPropertiesConfig;

import static org.assertj.core.api.Assertions.assertThat;

class InheritanceMetadataGenerationTests extends AbstractMetadataGenerationTests {

	@Test
	void childProperties() {
		ConfigurationMetadata metadata = compile(ChildPropertiesConfig.class);
		assertThat(metadata).has(Metadata.withGroup("inheritance").fromSource(ChildPropertiesConfig.class));
		assertThat(metadata).has(Metadata.withGroup("inheritance.nest").fromSource(ChildProperties.class));
		assertThat(metadata).has(Metadata.withGroup("inheritance.child-nest").fromSource(ChildProperties.class));
		assertThat(metadata).has(Metadata.withProperty("inheritance.bool-value"));
		assertThat(metadata).has(Metadata.withProperty("inheritance.int-value"));
		assertThat(metadata).has(Metadata.withProperty("inheritance.long-value"));
		assertThat(metadata).has(Metadata.withProperty("inheritance.nest.bool-value"));
		assertThat(metadata).has(Metadata.withProperty("inheritance.nest.int-value"));
		assertThat(metadata).has(Metadata.withProperty("inheritance.child-nest.bool-value"));
		assertThat(metadata).has(Metadata.withProperty("inheritance.child-nest.int-value"));
	}

	@Test
	void overrideChildProperties() {
		ConfigurationMetadata metadata = compile(OverrideChildPropertiesConfig.class);
		assertThat(metadata).has(Metadata.withGroup("inheritance").fromSource(OverrideChildPropertiesConfig.class));
		assertThat(metadata).has(Metadata.withGroup("inheritance.nest").fromSource(OverrideChildProperties.class));
		assertThat(metadata).has(Metadata.withProperty("inheritance.bool-value"));
		assertThat(metadata).has(Metadata.withProperty("inheritance.int-value"));
		assertThat(metadata).has(Metadata.withProperty("inheritance.long-value"));
		assertThat(metadata).has(Metadata.withProperty("inheritance.nest.bool-value"));
		assertThat(metadata).has(Metadata.withProperty("inheritance.nest.int-value"));
		assertThat(metadata).has(Metadata.withProperty("inheritance.nest.long-value"));

	}

}
