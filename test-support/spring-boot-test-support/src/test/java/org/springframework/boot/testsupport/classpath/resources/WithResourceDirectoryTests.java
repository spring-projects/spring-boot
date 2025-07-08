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

package org.springframework.boot.testsupport.classpath.resources;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link WithResourceDirectory}.
 *
 * @author Andy Wilkinson
 */
class WithResourceDirectoryTests {

	@Test
	@WithResourceDirectory("test")
	void whenWithResourceDirectoryIsUsedOnAMethodThenDirectoryIsCreated() throws IOException {
		assertThat(new ClassPathResource("test").getFile()).isDirectory();
	}

	@Test
	@WithResourceDirectory("com/example/nested")
	void whenWithResourceDirectoryNamesANestedDirectoryThenDirectoryIsCreated() throws IOException {
		assertThat(new ClassPathResource("com/example/nested").getFile()).isDirectory();
	}

	@Test
	@WithResourceDirectory("1")
	@WithResourceDirectory("2")
	@WithResourceDirectory("3")
	void whenWithResourceDirectoryIsRepeatedOnAMethodThenAllResourceDirectoriesAreCreated() throws IOException {
		assertThat(new ClassPathResource("1").getFile()).isDirectory();
		assertThat(new ClassPathResource("2").getFile()).isDirectory();
		assertThat(new ClassPathResource("3").getFile()).isDirectory();
	}

}
