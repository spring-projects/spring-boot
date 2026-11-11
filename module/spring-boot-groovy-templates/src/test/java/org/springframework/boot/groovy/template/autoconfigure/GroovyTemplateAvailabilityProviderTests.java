/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.groovy.template.autoconfigure;

import org.junit.jupiter.api.Test;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeHint;
import org.springframework.beans.factory.aot.AotServices;
import org.springframework.boot.autoconfigure.template.TemplateAvailabilityProvider;
import org.springframework.boot.groovy.template.autoconfigure.GroovyTemplateAvailabilityProvider.GroovyTemplateAvailabilityProperties;
import org.springframework.boot.groovy.template.autoconfigure.GroovyTemplateAvailabilityProvider.GroovyTemplateAvailabilityRuntimeHints;
import org.springframework.boot.testsupport.classpath.resources.WithResource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link GroovyTemplateAvailabilityProvider}.
 *
 * @author Andy Wilkinson
 */
class GroovyTemplateAvailabilityProviderTests {

	private final TemplateAvailabilityProvider provider = new GroovyTemplateAvailabilityProvider();

	private final ResourceLoader resourceLoader = new DefaultResourceLoader();

	private final MockEnvironment environment = new MockEnvironment();

	@Test
	@WithResource(name = "templates/home.tpl")
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
	@WithResource(name = "custom-templates/custom.tpl")
	void availabilityOfTemplateWithCustomLoaderPath() {
		this.environment.setProperty("spring.groovy.template.resource-loader-path", "classpath:/custom-templates/");
		assertThat(this.provider.isTemplateAvailable("custom", this.environment, getClass().getClassLoader(),
				this.resourceLoader))
			.isTrue();
	}

	@Test
	@WithResource(name = "custom-templates/custom.tpl")
	void availabilityOfTemplateWithCustomLoaderPathConfiguredAsAList() {
		this.environment.setProperty("spring.groovy.template.resource-loader-path[0]", "classpath:/custom-templates/");
		assertThat(this.provider.isTemplateAvailable("custom", this.environment, getClass().getClassLoader(),
				this.resourceLoader))
			.isTrue();
	}

	@Test
	@WithResource(name = "templates/prefix/prefixed.tpl")
	void availabilityOfTemplateWithCustomPrefix() {
		this.environment.setProperty("spring.groovy.template.prefix", "prefix/");
		assertThat(this.provider.isTemplateAvailable("prefixed", this.environment, getClass().getClassLoader(),
				this.resourceLoader))
			.isTrue();
	}

	@Test
	@WithResource(name = "templates/suffixed.groovytemplate")
	void availabilityOfTemplateWithCustomSuffix() {
		this.environment.setProperty("spring.groovy.template.suffix", ".groovytemplate");
		assertThat(this.provider.isTemplateAvailable("suffixed", this.environment, getClass().getClassLoader(),
				this.resourceLoader))
			.isTrue();
	}

	@Test
	void shouldRegisterGroovyTemplateAvailabilityPropertiesRuntimeHints() {
		assertThat(AotServices.factories().load(RuntimeHintsRegistrar.class))
			.hasAtLeastOneElementOfType(GroovyTemplateAvailabilityRuntimeHints.class);
		RuntimeHints hints = new RuntimeHints();
		new GroovyTemplateAvailabilityRuntimeHints().registerHints(hints, getClass().getClassLoader());
		TypeHint typeHint = hints.reflection().getTypeHint(GroovyTemplateAvailabilityProperties.class);
		assertThat(typeHint).isNotNull();
	}

}
