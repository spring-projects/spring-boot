/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.web.context.servlet;

import java.io.File;

import jakarta.servlet.ServletContext;
import org.junit.jupiter.api.Test;

import org.springframework.boot.io.ApplicationResourceLoader;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.support.ServletContextResource;
import org.springframework.web.context.support.ServletContextResourceLoader;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link ServletContextResourceFilePathResolver}.
 *
 * @author Phillip Webb
 */
class ServletContextResourceFilePathResolverIntegrationTests {

	@Test
	void getResourceWithPreferFileResolutionWhenPathWithServletContextResource() throws Exception {
		ServletContext servletContext = new MockServletContext();
		ServletContextResourceLoader servletContextResourceLoader = new ServletContextResourceLoader(servletContext);
		ResourceLoader loader = ApplicationResourceLoader.get(servletContextResourceLoader, true);
		Resource resource = loader.getResource("src/main/resources/a-file");
		assertThat(resource).isInstanceOf(FileSystemResource.class);
		assertThat(resource.getFile().getAbsoluteFile())
			.isEqualTo(new File("src/main/resources/a-file").getAbsoluteFile());
		ResourceLoader regularLoader = ApplicationResourceLoader.get(servletContextResourceLoader, false);
		assertThat(regularLoader.getResource("src/main/resources/a-file")).isInstanceOf(ServletContextResource.class);
	}

}
