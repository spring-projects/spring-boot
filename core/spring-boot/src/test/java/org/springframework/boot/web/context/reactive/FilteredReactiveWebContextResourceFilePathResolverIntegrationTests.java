/*
 * Copyright 2012-present the original author or authors.
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
