/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.configurationprocessor;

import org.junit.jupiter.api.Test;

import org.springframework.boot.configurationprocessor.metadata.ConfigurationMetadata;
import org.springframework.boot.configurationprocessor.metadata.Metadata;
import org.springframework.boot.configurationsample.name.ConstructorParameterNameAnnotationProperties;
import org.springframework.boot.configurationsample.name.JavaBeanNameAnnotationProperties;
import org.springframework.boot.configurationsample.name.LombokNameAnnotationProperties;
import org.springframework.boot.configurationsample.name.RecordComponentNameAnnotationProperties;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Metadata generation tests for using {@code @Name}.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 */
class NameAnnotationPropertiesTests extends AbstractMetadataGenerationTests {

	@Test
	void constructorParameterNameAnnotationProperties() {
		ConfigurationMetadata metadata = compile(ConstructorParameterNameAnnotationProperties.class);
		assertThat(metadata)
			.has(Metadata.withProperty("named.import", String.class)
				.fromSource(ConstructorParameterNameAnnotationProperties.class)
				.withDescription("Imports to apply."))
			.has(Metadata.withProperty("named.default", Boolean.class)
				.fromSource(ConstructorParameterNameAnnotationProperties.class)
				.withDefaultValue("Whether default mode is enabled.")
				.withDefaultValue(true));
	}

	@Test
	void recordComponentNameAnnotationProperties() {
		ConfigurationMetadata metadata = compile(RecordComponentNameAnnotationProperties.class);
		assertThat(metadata)
			.has(Metadata.withProperty("named.import", String.class)
				.fromSource(RecordComponentNameAnnotationProperties.class)
				.withDescription("Imports to apply."))
			.has(Metadata.withProperty("named.default", Boolean.class)
				.fromSource(RecordComponentNameAnnotationProperties.class)
				.withDefaultValue("Whether default mode is enabled.")
				.withDefaultValue(true));
	}

	@Test
	void javaBeanNameAnnotationProperties() {
		ConfigurationMetadata metadata = compile(JavaBeanNameAnnotationProperties.class);
		assertThat(metadata)
			.has(Metadata.withProperty("named.import", String.class)
				.fromSource(JavaBeanNameAnnotationProperties.class)
				.withDescription("Imports to apply."))
			.has(Metadata.withProperty("named.default", Boolean.class)
				.fromSource(JavaBeanNameAnnotationProperties.class)
				.withDefaultValue("Whether default mode is enabled.")
				.withDefaultValue(true));
	}

	@Test
	void lombokNameAnnotationProperties() {
		ConfigurationMetadata metadata = compile(LombokNameAnnotationProperties.class);
		assertThat(metadata)
			.has(Metadata.withProperty("named.import", String.class)
				.fromSource(LombokNameAnnotationProperties.class)
				.withDescription("Imports to apply."))
			.has(Metadata.withProperty("named.default", Boolean.class)
				.fromSource(LombokNameAnnotationProperties.class)
				.withDefaultValue("Whether default mode is enabled.")
				.withDefaultValue(true));
	}

}
