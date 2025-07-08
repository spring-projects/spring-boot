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
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link WithResource} when used on a super-class.
 *
 * @author Andy Wilkinson
 */
class OnSuperClassWithResourceTests extends WithResourceClass {

	@Test
	void whenWithResourceIsUsedOnASuperClassThenResourceIsAvailable() throws IOException {
		assertThat(new ClassPathResource("on-super-class").getContentAsString(StandardCharsets.UTF_8))
			.isEqualTo("super-class content");
	}

	@Test
	@WithResource(name = "method-resource", content = "method")
	void whenWithResourceIsUsedOnASuperClassAndMethodThenBothResourcesAreAvailable() throws IOException {
		assertThat(new ClassPathResource("on-super-class").getContentAsString(StandardCharsets.UTF_8))
			.isEqualTo("super-class content");
		assertThat(new ClassPathResource("method-resource").getContentAsString(StandardCharsets.UTF_8))
			.isEqualTo("method");
	}

	@Test
	@WithResource(name = "method-resource-1", content = "method-1")
	@WithResource(name = "method-resource-2", content = "method-2")
	void whenWithResourceIsUsedOnASuperClassAndRepeatedOnMethodThenAllResourcesAreAvailable() throws IOException {
		assertThat(new ClassPathResource("on-super-class").getContentAsString(StandardCharsets.UTF_8))
			.isEqualTo("super-class content");
		assertThat(new ClassPathResource("method-resource-1").getContentAsString(StandardCharsets.UTF_8))
			.isEqualTo("method-1");
		assertThat(new ClassPathResource("method-resource-2").getContentAsString(StandardCharsets.UTF_8))
			.isEqualTo("method-2");
	}

}
