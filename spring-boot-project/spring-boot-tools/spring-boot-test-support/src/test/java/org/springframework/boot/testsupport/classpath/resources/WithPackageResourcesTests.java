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

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link WithPackageResources}.
 *
 * @author Andy Wilkinson
 */
class WithPackageResourcesTests {

	@Test
	@WithPackageResources({ "resource-1.txt", "resource-2.txt", "sub/resource-3.txt" })
	void whenWithPackageResourcesIsUsedOnAMethodThenResourcesAreAvailable() throws IOException {
		assertThat(new ClassPathResource("resource-1.txt").getContentAsString(StandardCharsets.UTF_8)).isEqualTo("one");
		assertThat(new ClassPathResource("resource-2.txt").getContentAsString(StandardCharsets.UTF_8)).isEqualTo("two");
		assertThat(new ClassPathResource("sub/resource-3.txt").getContentAsString(StandardCharsets.UTF_8))
			.isEqualTo("three");
	}

	@Test
	@WithPackageResources("sub/resource-3.txt")
	void whenWithPackageResourcesOnlyIncludesSomeResourcesThenOnlyIncludedResourcesAreAvailable() throws IOException {
		assertThat(new ClassPathResource("resource-1.txt").exists()).isFalse();
		assertThat(new ClassPathResource("resource-2.txt").exists()).isFalse();
		assertThat(new ClassPathResource("sub/resource-3.txt").getContentAsString(StandardCharsets.UTF_8))
			.isEqualTo("three");
	}

}
