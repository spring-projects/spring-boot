/*
 * Copyright 2012-present the original author or authors.
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
