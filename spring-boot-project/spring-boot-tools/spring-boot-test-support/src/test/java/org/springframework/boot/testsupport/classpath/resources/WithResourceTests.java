/*
 * Copyright 2012-2025 the original author or authors.
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

package org.springframework.boot.testsupport.classpath.resources;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link WithResource}.
 *
 * @author Andy Wilkinson
 */
class WithResourceTests {

	@Test
	@WithResource(name = "test", content = "content")
	void whenWithResourceIsUsedOnAMethodThenResourceIsAvailable() throws IOException {
		assertThat(new ClassPathResource("test").getContentAsString(StandardCharsets.UTF_8)).isEqualTo("content");
	}

	@Test
	@WithResource(name = "test", content = "content")
	void whenWithResourceIsUsedOnAMethodThenResourceIsAvailableFromFileResourcesRoot(@ResourcesRoot File root) {
		assertThat(new File(root, "test")).hasContent("content");
	}

	@Test
	@WithResource(name = "test", content = "content")
	void whenWithResourceIsUsedOnAMethodThenResourceIsAvailableFromPathResourcesRoot(@ResourcesRoot Path root) {
		assertThat(root.resolve("test")).hasContent("content");
	}

	@Test
	@WithResource(name = "test", content = "content")
	void whenWithResourceIsUsedOnAMethodThenResourceIsAvailableFromPathResourcePath(
			@ResourcePath("test") Path resource) {
		assertThat(resource).hasContent("content");
	}

	@Test
	@WithResource(name = "test", content = "content")
	void whenWithResourceIsUsedOnAMethodThenResourceIsAvailableFromFileResourcePath(
			@ResourcePath("test") File resource) {
		assertThat(resource).hasContent("content");
	}

	@Test
	@WithResource(name = "test", content = "content")
	void whenWithResourceIsUsedOnAMethodThenResourceIsAvailableFromStringResourcePath(
			@ResourcePath("test") String resource) {
		assertThat(new File(resource)).hasContent("content");
	}

	@Test
	@WithResource(name = "test", content = "content")
	void whenWithResourceIsUsedOnAMethodThenResourceContentIsAvailableAsAString(
			@ResourceContent("test") String content) {
		assertThat(content).isEqualTo("content");
	}

	@Test
	@WithResource(name = "com/example/test-resource", content = "content")
	void whenWithResourceNameIncludesADirectoryThenResourceIsAvailable() throws IOException {
		assertThat(new ClassPathResource("com/example/test-resource").getContentAsString(StandardCharsets.UTF_8))
			.isEqualTo("content");
	}

	@Test
	@WithResource(name = "1", content = "one")
	@WithResource(name = "2", content = "two")
	@WithResource(name = "3", content = "three")
	void whenWithResourceIsRepeatedOnAMethodThenAllResourcesAreAvailable() throws IOException {
		assertThat(new ClassPathResource("1").getContentAsString(StandardCharsets.UTF_8)).isEqualTo("one");
		assertThat(new ClassPathResource("2").getContentAsString(StandardCharsets.UTF_8)).isEqualTo("two");
		assertThat(new ClassPathResource("3").getContentAsString(StandardCharsets.UTF_8)).isEqualTo("three");
	}

	@Test
	@WithResource(name = "org/springframework/boot/testsupport/classpath/resources/resource-1.txt",
			content = "from-with-resource")
	void whenWithResourceCreatesResourceThatIsAvailableElsewhereBothResourcesCanBeLoaded() throws IOException {
		Resource[] resources = new PathMatchingResourcePatternResolver()
			.getResources("classpath*:org/springframework/boot/testsupport/classpath/resources/resource-1.txt");
		assertThat(resources).hasSize(2);
		assertThat(resources).extracting((resource) -> resource.getContentAsString(StandardCharsets.UTF_8))
			.containsExactly("from-with-resource", "one");
	}

	@Test
	@WithResource(name = "org/springframework/boot/testsupport/classpath/resources/resource-1.txt",
			content = "from-with-resource", additional = false)
	void whenWithResourceCreatesResourceThatIsNotAdditionalThenResourceThatIsAvailableElsewhereCannotBeLoaded()
			throws IOException {
		Resource[] resources = new PathMatchingResourcePatternResolver()
			.getResources("classpath*:org/springframework/boot/testsupport/classpath/resources/resource-1.txt");
		assertThat(resources).hasSize(1);
		assertThat(resources[0].getContentAsString(StandardCharsets.UTF_8)).isEqualTo("from-with-resource");
	}

}
