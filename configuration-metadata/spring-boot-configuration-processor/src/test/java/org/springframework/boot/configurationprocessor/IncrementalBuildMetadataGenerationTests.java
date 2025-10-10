/*
 * Copyright 2012-present the original author or authors.
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
import org.springframework.boot.configurationsample.incremental.BarProperties;
import org.springframework.boot.configurationsample.incremental.FooProperties;
import org.springframework.boot.configurationsample.incremental.RenamedBarProperties;
import org.springframework.boot.configurationsample.simple.ClassWithNestedProperties;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Metadata generation tests for incremental builds.
 *
 * @author Stephane Nicoll
 * @author Scott Frederick
 */
class IncrementalBuildMetadataGenerationTests extends AbstractMetadataGenerationTests {

	@Test
	void incrementalBuild() throws Exception {
		TestProject project = new TestProject(FooProperties.class, BarProperties.class);
		ConfigurationMetadata metadata = project.compile();
		assertThat(metadata)
			.has(Metadata.withProperty("foo.counter").fromSource(FooProperties.class).withDefaultValue(0));
		assertThat(metadata)
			.has(Metadata.withProperty("bar.counter").fromSource(BarProperties.class).withDefaultValue(0));
		metadata = project.compile();
		assertThat(metadata)
			.has(Metadata.withProperty("foo.counter").fromSource(FooProperties.class).withDefaultValue(0));
		assertThat(metadata)
			.has(Metadata.withProperty("bar.counter").fromSource(BarProperties.class).withDefaultValue(0));
		project.addSourceCode(BarProperties.class, BarProperties.class.getResourceAsStream("BarProperties.snippet"));
		metadata = project.compile();
		assertThat(metadata).has(Metadata.withProperty("bar.extra"));
		assertThat(metadata).has(Metadata.withProperty("foo.counter").withDefaultValue(0));
		assertThat(metadata).has(Metadata.withProperty("bar.counter").withDefaultValue(0));
		project.revert(BarProperties.class);
		metadata = project.compile();
		assertThat(metadata).isNotEqualTo(Metadata.withProperty("bar.extra"));
		assertThat(metadata).has(Metadata.withProperty("foo.counter").withDefaultValue(0));
		assertThat(metadata).has(Metadata.withProperty("bar.counter").withDefaultValue(0));
	}

	@Test
	void incrementalBuildAnnotationRemoved() {
		TestProject project = new TestProject(FooProperties.class, BarProperties.class);
		ConfigurationMetadata metadata = project.compile();
		assertThat(metadata).has(Metadata.withProperty("foo.counter").withDefaultValue(0));
		assertThat(metadata).has(Metadata.withProperty("bar.counter").withDefaultValue(0));
		project.replaceText(BarProperties.class, "@TestConfigurationProperties", "//@TestConfigurationProperties");
		project.replaceText(FooProperties.class, "@TestConfigurationProperties", "//@TestConfigurationProperties");
		metadata = project.compile();
		assertThat(metadata).isNull();
	}

	@Test
	void incrementalBuildTypeRenamed() {
		TestProject project = new TestProject(FooProperties.class, BarProperties.class);
		ConfigurationMetadata metadata = project.compile();
		assertThat(metadata)
			.has(Metadata.withProperty("foo.counter").fromSource(FooProperties.class).withDefaultValue(0));
		assertThat(metadata)
			.has(Metadata.withProperty("bar.counter").fromSource(BarProperties.class).withDefaultValue(0));
		assertThat(metadata).doesNotHave(Metadata.withProperty("bar.counter").fromSource(RenamedBarProperties.class));
		project.delete(BarProperties.class);
		project.add(RenamedBarProperties.class);
		metadata = project.compile(metadata);
		assertThat(metadata)
			.has(Metadata.withProperty("foo.counter").fromSource(FooProperties.class).withDefaultValue(0));
		assertThat(metadata)
			.doesNotHave(Metadata.withProperty("bar.counter").fromSource(BarProperties.class).withDefaultValue(0));
		assertThat(metadata)
			.has(Metadata.withProperty("bar.counter").withDefaultValue(0).fromSource(RenamedBarProperties.class));
	}

	@Test
	void incrementalBuildDoesNotDeleteItems() {
		TestProject project = new TestProject(ClassWithNestedProperties.class, FooProperties.class);
		ConfigurationMetadata initialMetadata = project.compile();
		ConfigurationMetadata updatedMetadata = project.compile();
		assertThat(initialMetadata.getItems()).isEqualTo(updatedMetadata.getItems());
	}

}
