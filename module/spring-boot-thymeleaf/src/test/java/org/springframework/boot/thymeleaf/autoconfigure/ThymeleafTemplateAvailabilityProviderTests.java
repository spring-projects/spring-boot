/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.thymeleaf.autoconfigure;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.template.TemplateAvailabilityProvider;
import org.springframework.boot.testsupport.classpath.resources.WithResource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ThymeleafTemplateAvailabilityProvider}.
 *
 * @author Andy Wilkinson
 */
class ThymeleafTemplateAvailabilityProviderTests {

	private final TemplateAvailabilityProvider provider = new ThymeleafTemplateAvailabilityProvider();

	private final ResourceLoader resourceLoader = new DefaultResourceLoader();

	private final MockEnvironment environment = new MockEnvironment();

	@Test
	@WithResource(name = "templates/home.html")
	void availabilityOfTemplateInDefaultLocation() {
		assertThat(this.provider.isTemplateAvailable("home", this.environment, getClass().getClassLoader(),
				this.resourceLoader))
			.isTrue();
	}

	@Test
	void availabilityOfTemplateThatDoesNotExist() {
		assertThat(this.provider.isTemplateAvailable("whatever", this.environment, getClass().getClassLoader(),
				this.resourceLoader))
			.isFalse();
	}

	@Test
	@WithResource(name = "custom-templates/custom.html")
	void availabilityOfTemplateWithCustomPrefix() {
		this.environment.setProperty("spring.thymeleaf.prefix", "classpath:/custom-templates/");
		assertThat(this.provider.isTemplateAvailable("custom", this.environment, getClass().getClassLoader(),
				this.resourceLoader))
			.isTrue();
	}

	@Test
	@WithResource(name = "templates/suffixed.thymeleaf")
	void availabilityOfTemplateWithCustomSuffix() {
		this.environment.setProperty("spring.thymeleaf.suffix", ".thymeleaf");
		assertThat(this.provider.isTemplateAvailable("suffixed", this.environment, getClass().getClassLoader(),
				this.resourceLoader))
			.isTrue();
	}

}
