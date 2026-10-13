/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.webmvc.autoconfigure;

import org.junit.jupiter.api.Test;

import org.springframework.boot.testsupport.classpath.resources.WithResource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link JspTemplateAvailabilityProvider}.
 *
 * @author Yunkun Huang
 */
class JspTemplateAvailabilityProviderTests {

	private final JspTemplateAvailabilityProvider provider = new JspTemplateAvailabilityProvider();

	private final ResourceLoader resourceLoader = new DefaultResourceLoader();

	private final MockEnvironment environment = new MockEnvironment();

	@Test
	void availabilityOfTemplateThatDoesNotExist() {
		assertThat(isTemplateAvailable("whatever")).isFalse();
	}

	@Test
	@WithResource(name = "custom-templates/custom.jsp")
	void availabilityOfTemplateWithCustomPrefix() {
		this.environment.setProperty("spring.mvc.view.prefix", "classpath:/custom-templates/");
		assertThat(isTemplateAvailable("custom.jsp")).isTrue();
	}

	@Test
	@WithResource(name = "suffixed.java-server-pages")
	void availabilityOfTemplateWithCustomSuffix() {
		this.environment.setProperty("spring.mvc.view.suffix", ".java-server-pages");
		assertThat(isTemplateAvailable("suffixed")).isTrue();
	}

	private boolean isTemplateAvailable(String view) {
		return this.provider.isTemplateAvailable(view, this.environment, getClass().getClassLoader(),
				this.resourceLoader);
	}

}
