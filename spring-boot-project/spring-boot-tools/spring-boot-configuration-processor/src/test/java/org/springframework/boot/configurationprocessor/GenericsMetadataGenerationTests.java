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
import org.springframework.boot.configurationsample.generic.AbstractGenericProperties;
import org.springframework.boot.configurationsample.generic.GenericConfig;
import org.springframework.boot.configurationsample.generic.SimpleGenericProperties;
import org.springframework.boot.configurationsample.generic.UnresolvedGenericProperties;
import org.springframework.boot.configurationsample.generic.WildcardConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Metadata generation tests for generics handling.
 *
 * @author Stephane Nicoll
 */
public class GenericsMetadataGenerationTests extends AbstractMetadataGenerationTests {

	@Test
	public void simpleGenericProperties() {
		ConfigurationMetadata metadata = compile(AbstractGenericProperties.class,
				SimpleGenericProperties.class);
		assertThat(metadata).has(
				Metadata.withGroup("generic").fromSource(SimpleGenericProperties.class));
		assertThat(metadata).has(Metadata.withProperty("generic.name", String.class)
				.fromSource(SimpleGenericProperties.class)
				.withDescription("Generic name.").withDefaultValue(null));
		assertThat(metadata).has(Metadata
				.withProperty("generic.mappings",
						"java.util.Map<java.lang.Integer,java.time.Duration>")
				.fromSource(SimpleGenericProperties.class)
				.withDescription("Generic mappings.").withDefaultValue(null));
		assertThat(metadata.getItems()).hasSize(3);
	}

	@Test
	public void unresolvedGenericProperties() {
		ConfigurationMetadata metadata = compile(AbstractGenericProperties.class,
				UnresolvedGenericProperties.class);
		assertThat(metadata).has(Metadata.withGroup("generic")
				.fromSource(UnresolvedGenericProperties.class));
		assertThat(metadata).has(Metadata.withProperty("generic.name", String.class)
				.fromSource(UnresolvedGenericProperties.class)
				.withDescription("Generic name.").withDefaultValue(null));
		assertThat(metadata).has(Metadata
				.withProperty("generic.mappings",
						"java.util.Map<java.lang.Number,java.lang.Object>")
				.fromSource(UnresolvedGenericProperties.class)
				.withDescription("Generic mappings.").withDefaultValue(null));
		assertThat(metadata.getItems()).hasSize(3);
	}

	@Test
	public void genericTypes() {
		ConfigurationMetadata metadata = compile(GenericConfig.class);
		assertThat(metadata).has(Metadata.withGroup("generic").ofType(
				"org.springframework.boot.configurationsample.generic.GenericConfig"));
		assertThat(metadata).has(Metadata.withGroup("generic.foo").ofType(
				"org.springframework.boot.configurationsample.generic.GenericConfig$Foo"));
		assertThat(metadata).has(Metadata.withGroup("generic.foo.bar").ofType(
				"org.springframework.boot.configurationsample.generic.GenericConfig$Bar"));
		assertThat(metadata).has(Metadata.withGroup("generic.foo.bar.biz").ofType(
				"org.springframework.boot.configurationsample.generic.GenericConfig$Bar$Biz"));
		assertThat(metadata).has(Metadata.withProperty("generic.foo.name")
				.ofType(String.class).fromSource(GenericConfig.Foo.class));
		assertThat(metadata).has(Metadata.withProperty("generic.foo.string-to-bar")
				.ofType("java.util.Map<java.lang.String,org.springframework.boot.configurationsample.generic.GenericConfig$Bar<java.lang.Integer>>")
				.fromSource(GenericConfig.Foo.class));
		assertThat(metadata).has(Metadata.withProperty("generic.foo.string-to-integer")
				.ofType("java.util.Map<java.lang.String,java.lang.Integer>")
				.fromSource(GenericConfig.Foo.class));
		assertThat(metadata).has(Metadata.withProperty("generic.foo.bar.name")
				.ofType("java.lang.String").fromSource(GenericConfig.Bar.class));
		assertThat(metadata).has(Metadata.withProperty("generic.foo.bar.biz.name")
				.ofType("java.lang.String").fromSource(GenericConfig.Bar.Biz.class));
		assertThat(metadata.getItems()).hasSize(9);
	}

	@Test
	public void wildcardTypes() {
		ConfigurationMetadata metadata = compile(WildcardConfig.class);
		assertThat(metadata)
				.has(Metadata.withGroup("wildcard").ofType(WildcardConfig.class));
		assertThat(metadata).has(Metadata.withProperty("wildcard.string-to-number")
				.ofType("java.util.Map<java.lang.String,? extends java.lang.Number>")
				.fromSource(WildcardConfig.class));
		assertThat(metadata).has(Metadata.withProperty("wildcard.integers")
				.ofType("java.util.List<? super java.lang.Integer>")
				.fromSource(WildcardConfig.class));
		assertThat(metadata.getItems()).hasSize(3);
	}

}
