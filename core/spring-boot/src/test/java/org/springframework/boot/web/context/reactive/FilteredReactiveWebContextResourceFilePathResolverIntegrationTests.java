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

package org.springframework.boot.web.context.reactive;

import java.io.File;

import org.junit.jupiter.api.Test;

import org.springframework.boot.io.ApplicationResourceLoader;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link FilteredReactiveWebContextResourceFilePathResolver}.
 *
 * @author Dmytro Nosan
 */
class FilteredReactiveWebContextResourceFilePathResolverIntegrationTests {

	@Test
	void getResourceWhenFilteredReactiveWebContextResourceWithPreferFileResolution() throws Exception {
		ResourceLoader resourceLoader = ApplicationResourceLoader
			.get(new AnnotationConfigReactiveWebApplicationContext(), true);
		Resource resource = resourceLoader.getResource("src/main/resources/a-file");
		assertThat(resource).isInstanceOf(FileSystemResource.class);
		assertThat(resource.getFile().getAbsoluteFile())
			.isEqualTo(new File("src/main/resources/a-file").getAbsoluteFile());
	}

	@Test
	void getResourceWhenFilteredReactiveWebContextResource() {
		ResourceLoader resourceLoader = ApplicationResourceLoader
			.get(new AnnotationConfigReactiveWebApplicationContext(), false);
		Resource resource = resourceLoader.getResource("src/main/resources/a-file");
		assertThat(resource).isInstanceOf(ClassUtils.resolveClassName(
				"org.springframework.boot.web.context.reactive.FilteredReactiveWebContextResource", null));
	}

}
