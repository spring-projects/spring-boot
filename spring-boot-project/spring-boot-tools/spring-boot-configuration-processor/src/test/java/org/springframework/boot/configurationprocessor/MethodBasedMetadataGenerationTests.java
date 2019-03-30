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

import org.junit.Test;

import org.springframework.boot.configurationprocessor.metadata.ConfigurationMetadata;
import org.springframework.boot.configurationprocessor.metadata.Metadata;
import org.springframework.boot.configurationsample.method.DeprecatedMethodConfig;
import org.springframework.boot.configurationsample.method.EmptyTypeMethodConfig;
import org.springframework.boot.configurationsample.method.InvalidMethodConfig;
import org.springframework.boot.configurationsample.method.MethodAndClassConfig;
import org.springframework.boot.configurationsample.method.SimpleMethodConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Metadata generation tests for types defined by {@code @Bean} methods.
 *
 * @author Stephane Nicoll
 */
public class MethodBasedMetadataGenerationTests extends AbstractMetadataGenerationTests {

	@Test
	public void simpleMethodConfig() {
		ConfigurationMetadata metadata = compile(SimpleMethodConfig.class);
		assertThat(metadata)
				.has(Metadata.withGroup("foo").fromSource(SimpleMethodConfig.class));
		assertThat(metadata).has(Metadata.withProperty("foo.name", String.class)
				.fromSource(SimpleMethodConfig.Foo.class));
		assertThat(metadata).has(Metadata.withProperty("foo.flag", Boolean.class)
				.withDefaultValue(false).fromSource(SimpleMethodConfig.Foo.class));
	}

	@Test
	public void invalidMethodConfig() {
		ConfigurationMetadata metadata = compile(InvalidMethodConfig.class);
		assertThat(metadata).has(Metadata.withProperty("something.name", String.class)
				.fromSource(InvalidMethodConfig.class));
		assertThat(metadata).isNotEqualTo(Metadata.withProperty("invalid.name"));
	}

	@Test
	public void methodAndClassConfig() {
		ConfigurationMetadata metadata = compile(MethodAndClassConfig.class);
		assertThat(metadata).has(Metadata.withProperty("conflict.name", String.class)
				.fromSource(MethodAndClassConfig.Foo.class));
		assertThat(metadata).has(Metadata.withProperty("conflict.flag", Boolean.class)
				.withDefaultValue(false).fromSource(MethodAndClassConfig.Foo.class));
		assertThat(metadata).has(Metadata.withProperty("conflict.value", String.class)
				.fromSource(MethodAndClassConfig.class));
	}

	@Test
	public void emptyTypeMethodConfig() {
		ConfigurationMetadata metadata = compile(EmptyTypeMethodConfig.class);
		assertThat(metadata).isNotEqualTo(Metadata.withProperty("something.foo"));
	}

	@Test
	public void deprecatedMethodConfig() {
		Class<DeprecatedMethodConfig> type = DeprecatedMethodConfig.class;
		ConfigurationMetadata metadata = compile(type);
		assertThat(metadata).has(Metadata.withGroup("foo").fromSource(type));
		assertThat(metadata).has(Metadata.withProperty("foo.name", String.class)
				.fromSource(DeprecatedMethodConfig.Foo.class)
				.withDeprecation(null, null));
		assertThat(metadata).has(Metadata.withProperty("foo.flag", Boolean.class)
				.withDefaultValue(false).fromSource(DeprecatedMethodConfig.Foo.class)
				.withDeprecation(null, null));
	}

	@Test
	@SuppressWarnings("deprecation")
	public void deprecatedMethodConfigOnClass() {
		Class<?> type = org.springframework.boot.configurationsample.method.DeprecatedClassMethodConfig.class;
		ConfigurationMetadata metadata = compile(type);
		assertThat(metadata).has(Metadata.withGroup("foo").fromSource(type));
		assertThat(metadata).has(Metadata.withProperty("foo.name", String.class)
				.fromSource(
						org.springframework.boot.configurationsample.method.DeprecatedClassMethodConfig.Foo.class)
				.withDeprecation(null, null));
		assertThat(metadata).has(Metadata.withProperty("foo.flag", Boolean.class)
				.withDefaultValue(false)
				.fromSource(
						org.springframework.boot.configurationsample.method.DeprecatedClassMethodConfig.Foo.class)
				.withDeprecation(null, null));
	}

}
