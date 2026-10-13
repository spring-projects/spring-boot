/*
 * Copyright 2012-present the original author or authors.
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
		assertThat(metadata).has(Metadata.withProperty("immutable.the-name", String.class)
			.fromSource(ImmutableSimpleProperties.class)
			.withDescription("The name of this simple properties.")
			.withDefaultValue("boot"));
		assertThat(metadata).has(Metadata.withProperty("immutable.flag", Boolean.class)
			.withDefaultValue(false)
			.fromSource(ImmutableSimpleProperties.class)
			.withDescription("A simple flag.")
			.withDeprecation());
		assertThat(metadata).has(Metadata.withProperty("immutable.comparator"));
		assertThat(metadata).has(Metadata.withProperty("immutable.counter"));
		assertThat(metadata.getItems()).hasSize(5);
	}

}
