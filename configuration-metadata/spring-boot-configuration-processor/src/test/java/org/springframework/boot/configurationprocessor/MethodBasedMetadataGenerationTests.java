/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.configurationprocessor;

import org.junit.jupiter.api.Test;

import org.springframework.boot.configurationprocessor.metadata.ConfigurationMetadata;
import org.springframework.boot.configurationprocessor.metadata.Metadata;
import org.springframework.boot.configurationsample.method.DeprecatedMethodConfig;
import org.springframework.boot.configurationsample.method.EmptyTypeMethodConfig;
import org.springframework.boot.configurationsample.method.InvalidMethodConfig;
import org.springframework.boot.configurationsample.method.MethodAndClassConfig;
import org.springframework.boot.configurationsample.method.PackagePrivateMethodConfig;
import org.springframework.boot.configurationsample.method.PrivateMethodConfig;
import org.springframework.boot.configurationsample.method.ProtectedMethodConfig;
import org.springframework.boot.configurationsample.method.PublicMethodConfig;
import org.springframework.boot.configurationsample.method.SingleConstructorMethodConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Metadata generation tests for types defined by {@code @Bean} methods.
 *
 * @author Stephane Nicoll
 * @author Scott Frederick
 */
class MethodBasedMetadataGenerationTests extends AbstractMetadataGenerationTests {

	@Test
	void publicMethodConfig() {
		methodConfig(PublicMethodConfig.class, PublicMethodConfig.Foo.class);
	}

	@Test
	void protectedMethodConfig() {
		methodConfig(ProtectedMethodConfig.class, ProtectedMethodConfig.Foo.class);
	}

	@Test
	void packagePrivateMethodConfig() {
		methodConfig(PackagePrivateMethodConfig.class, PackagePrivateMethodConfig.Foo.class);
	}

	private void methodConfig(Class<?> config, Class<?> properties) {
		ConfigurationMetadata metadata = compile(config);
		assertThat(metadata).has(Metadata.withGroup("foo").fromSource(config));
		assertThat(metadata).has(Metadata.withProperty("foo.name", String.class).fromSource(properties));
		assertThat(metadata)
			.has(Metadata.withProperty("foo.flag", Boolean.class).withDefaultValue(false).fromSource(properties));
	}

	@Test
	void privateMethodConfig() {
		ConfigurationMetadata metadata = compile(PrivateMethodConfig.class);
		assertThat(metadata).isNull();
	}

	@Test
	void invalidMethodConfig() {
		ConfigurationMetadata metadata = compile(InvalidMethodConfig.class);
		assertThat(metadata)
			.has(Metadata.withProperty("something.name", String.class).fromSource(InvalidMethodConfig.class));
		assertThat(metadata).isNotEqualTo(Metadata.withProperty("invalid.name"));
	}

	@Test
	void methodAndClassConfig() {
		ConfigurationMetadata metadata = compile(MethodAndClassConfig.class);
		assertThat(metadata)
			.has(Metadata.withProperty("conflict.name", String.class).fromSource(MethodAndClassConfig.Foo.class));
		assertThat(metadata).has(Metadata.withProperty("conflict.flag", Boolean.class)
			.withDefaultValue(false)
			.fromSource(MethodAndClassConfig.Foo.class));
		assertThat(metadata)
			.has(Metadata.withProperty("conflict.value", String.class).fromSource(MethodAndClassConfig.class));
	}

	@Test
	void singleConstructorMethodConfig() {
		ConfigurationMetadata metadata = compile(SingleConstructorMethodConfig.class);
		assertThat(metadata).doesNotHave(Metadata.withProperty("foo.my-service", Object.class)
			.fromSource(SingleConstructorMethodConfig.Foo.class));
		assertThat(metadata)
			.has(Metadata.withProperty("foo.name", String.class).fromSource(SingleConstructorMethodConfig.Foo.class));
		assertThat(metadata).has(Metadata.withProperty("foo.flag", Boolean.class)
			.withDefaultValue(false)
			.fromSource(SingleConstructorMethodConfig.Foo.class));
	}

	@Test
	void emptyTypeMethodConfig() {
		ConfigurationMetadata metadata = compile(EmptyTypeMethodConfig.class);
		assertThat(metadata).isNotEqualTo(Metadata.withProperty("something.foo"));
	}

	@Test
	void deprecatedMethodConfig() {
		Class<DeprecatedMethodConfig> type = DeprecatedMethodConfig.class;
		ConfigurationMetadata metadata = compile(type);
		assertThat(metadata).has(Metadata.withGroup("foo").fromSource(type));
		assertThat(metadata).has(Metadata.withProperty("foo.name", String.class)
			.fromSource(DeprecatedMethodConfig.Foo.class)
			.withDeprecation());
		assertThat(metadata).has(Metadata.withProperty("foo.flag", Boolean.class)
			.withDefaultValue(false)
			.fromSource(DeprecatedMethodConfig.Foo.class)
			.withDeprecation());
	}

	@Test
	@SuppressWarnings("deprecation")
	void deprecatedMethodConfigOnClass() {
		Class<?> type = org.springframework.boot.configurationsample.method.DeprecatedClassMethodConfig.class;
		ConfigurationMetadata metadata = compile(type);
		assertThat(metadata).has(Metadata.withGroup("foo").fromSource(type));
		assertThat(metadata).has(Metadata.withProperty("foo.name", String.class)
			.fromSource(org.springframework.boot.configurationsample.method.DeprecatedClassMethodConfig.Foo.class)
			.withDeprecation());
		assertThat(metadata).has(Metadata.withProperty("foo.flag", Boolean.class)
			.withDefaultValue(false)
			.fromSource(org.springframework.boot.configurationsample.method.DeprecatedClassMethodConfig.Foo.class)
			.withDeprecation());
	}

}
