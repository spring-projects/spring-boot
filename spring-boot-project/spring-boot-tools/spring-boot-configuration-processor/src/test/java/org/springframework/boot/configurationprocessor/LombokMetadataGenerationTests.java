/*
 * Copyright 2012-2021 the original author or authors.
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
import org.springframework.boot.configurationsample.lombok.LombokAccessLevelOverwriteDataProperties;
import org.springframework.boot.configurationsample.lombok.LombokAccessLevelOverwriteDefaultProperties;
import org.springframework.boot.configurationsample.lombok.LombokAccessLevelOverwriteExplicitProperties;
import org.springframework.boot.configurationsample.lombok.LombokAccessLevelProperties;
import org.springframework.boot.configurationsample.lombok.LombokExplicitProperties;
import org.springframework.boot.configurationsample.lombok.LombokInnerClassProperties;
import org.springframework.boot.configurationsample.lombok.LombokInnerClassWithGetterProperties;
import org.springframework.boot.configurationsample.lombok.LombokSimpleDataProperties;
import org.springframework.boot.configurationsample.lombok.LombokSimpleProperties;
import org.springframework.boot.configurationsample.lombok.LombokSimpleValueProperties;
import org.springframework.boot.configurationsample.lombok.SimpleLombokPojo;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Metadata generation tests for Lombok classes.
 *
 * @author Stephane Nicoll
 */
class LombokMetadataGenerationTests extends AbstractMetadataGenerationTests {

	@Test
	void lombokDataProperties() {
		ConfigurationMetadata metadata = compile(LombokSimpleDataProperties.class);
		assertSimpleLombokProperties(metadata, LombokSimpleDataProperties.class, "data");
	}

	@Test
	void lombokValueProperties() {
		ConfigurationMetadata metadata = compile(LombokSimpleValueProperties.class);
		assertSimpleLombokProperties(metadata, LombokSimpleValueProperties.class, "value");
	}

	@Test
	void lombokSimpleProperties() {
		ConfigurationMetadata metadata = compile(LombokSimpleProperties.class);
		assertSimpleLombokProperties(metadata, LombokSimpleProperties.class, "simple");
	}

	@Test
	void lombokExplicitProperties() {
		ConfigurationMetadata metadata = compile(LombokExplicitProperties.class);
		assertSimpleLombokProperties(metadata, LombokExplicitProperties.class, "explicit");
		assertThat(metadata.getItems()).hasSize(6);
	}

	@Test
	void lombokAccessLevelProperties() {
		ConfigurationMetadata metadata = compile(LombokAccessLevelProperties.class);
		assertAccessLevelLombokProperties(metadata, LombokAccessLevelProperties.class, "accesslevel", 2);
	}

	@Test
	void lombokAccessLevelOverwriteDataProperties() {
		ConfigurationMetadata metadata = compile(LombokAccessLevelOverwriteDataProperties.class);
		assertAccessLevelOverwriteLombokProperties(metadata, LombokAccessLevelOverwriteDataProperties.class,
				"accesslevel.overwrite.data");
	}

	@Test
	void lombokAccessLevelOverwriteExplicitProperties() {
		ConfigurationMetadata metadata = compile(LombokAccessLevelOverwriteExplicitProperties.class);
		assertAccessLevelOverwriteLombokProperties(metadata, LombokAccessLevelOverwriteExplicitProperties.class,
				"accesslevel.overwrite.explicit");
	}

	@Test
	void lombokAccessLevelOverwriteDefaultProperties() {
		ConfigurationMetadata metadata = compile(LombokAccessLevelOverwriteDefaultProperties.class);
		assertAccessLevelOverwriteLombokProperties(metadata, LombokAccessLevelOverwriteDefaultProperties.class,
				"accesslevel.overwrite.default");
	}

	@Test
	void lombokInnerClassProperties() {
		ConfigurationMetadata metadata = compile(LombokInnerClassProperties.class);
		assertThat(metadata).has(Metadata.withGroup("config").fromSource(LombokInnerClassProperties.class));
		assertThat(metadata).has(Metadata.withGroup("config.first").ofType(LombokInnerClassProperties.Foo.class)
				.fromSource(LombokInnerClassProperties.class));
		assertThat(metadata).has(Metadata.withProperty("config.first.name"));
		assertThat(metadata).has(Metadata.withProperty("config.first.bar.name"));
		assertThat(metadata).has(Metadata.withGroup("config.second", LombokInnerClassProperties.Foo.class)
				.fromSource(LombokInnerClassProperties.class));
		assertThat(metadata).has(Metadata.withProperty("config.second.name"));
		assertThat(metadata).has(Metadata.withProperty("config.second.bar.name"));
		assertThat(metadata).has(Metadata.withGroup("config.third").ofType(SimpleLombokPojo.class)
				.fromSource(LombokInnerClassProperties.class));
		// For some reason the annotation processor resolves a type for SimpleLombokPojo
		// that is resolved (compiled) and the source annotations are gone. Because we
		// don't see the @Data annotation anymore, no field is harvested. What is crazy is
		// that a sample project works fine so this seems to be related to the unit test
		// environment for some reason. assertThat(metadata,
		// containsProperty("config.third.value"));
		assertThat(metadata).has(Metadata.withProperty("config.fourth"));
		assertThat(metadata).isNotEqualTo(Metadata.withGroup("config.fourth"));
	}

	@Test
	void lombokInnerClassWithGetterProperties() {
		ConfigurationMetadata metadata = compile(LombokInnerClassWithGetterProperties.class);
		assertThat(metadata).has(Metadata.withGroup("config").fromSource(LombokInnerClassWithGetterProperties.class));
		assertThat(metadata)
				.has(Metadata.withGroup("config.first").ofType(LombokInnerClassWithGetterProperties.Foo.class)
						.fromSourceMethod("getFirst()").fromSource(LombokInnerClassWithGetterProperties.class));
		assertThat(metadata).has(Metadata.withProperty("config.first.name"));
		assertThat(metadata.getItems()).hasSize(3);
	}

	private void assertSimpleLombokProperties(ConfigurationMetadata metadata, Class<?> source, String prefix) {
		assertThat(metadata).has(Metadata.withGroup(prefix).fromSource(source));
		assertThat(metadata).doesNotHave(Metadata.withProperty(prefix + ".id"));
		assertThat(metadata).has(Metadata.withProperty(prefix + ".name", String.class).fromSource(source)
				.withDescription("Name description."));
		assertThat(metadata).has(Metadata.withProperty(prefix + ".description"));
		assertThat(metadata).has(Metadata.withProperty(prefix + ".counter"));
		assertThat(metadata).has(Metadata.withProperty(prefix + ".number").fromSource(source).withDefaultValue(0)
				.withDeprecation(null, null));
		assertThat(metadata).has(Metadata.withProperty(prefix + ".items"));
		assertThat(metadata).doesNotHave(Metadata.withProperty(prefix + ".ignored"));
	}

	private void assertAccessLevelOverwriteLombokProperties(ConfigurationMetadata metadata, Class<?> source,
			String prefix) {
		assertAccessLevelLombokProperties(metadata, source, prefix, 7);
	}

	private void assertAccessLevelLombokProperties(ConfigurationMetadata metadata, Class<?> source, String prefix,
			int countNameFields) {
		assertThat(metadata).has(Metadata.withGroup(prefix).fromSource(source));
		for (int i = 0; i < countNameFields; i++) {
			assertThat(metadata).has(Metadata.withProperty(prefix + ".name" + i, String.class));
		}
		assertThat(metadata.getItems()).hasSize(1 + countNameFields);
	}

}
