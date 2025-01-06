/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.autoconfigure.groovy.template;

import org.junit.jupiter.api.Test;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeHint;
import org.springframework.beans.factory.aot.AotServices;
import org.springframework.boot.autoconfigure.groovy.template.GroovyTemplateAvailabilityProvider.GroovyTemplateAvailabilityProperties;
import org.springframework.boot.autoconfigure.groovy.template.GroovyTemplateAvailabilityProvider.GroovyTemplateAvailabilityRuntimeHints;
import org.springframework.boot.autoconfigure.template.TemplateAvailabilityProvider;
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
	void availabilityOfTemplateWithCustomLoaderPath() {
		this.environment.setProperty("spring.groovy.template.resource-loader-path", "classpath:/custom-templates/");
		assertThat(this.provider.isTemplateAvailable("custom", this.environment, getClass().getClassLoader(),
				this.resourceLoader))
			.isTrue();
	}

	@Test
	void availabilityOfTemplateWithCustomLoaderPathConfiguredAsAList() {
		this.environment.setProperty("spring.groovy.template.resource-loader-path[0]", "classpath:/custom-templates/");
		assertThat(this.provider.isTemplateAvailable("custom", this.environment, getClass().getClassLoader(),
				this.resourceLoader))
			.isTrue();
	}

	@Test
	void availabilityOfTemplateWithCustomPrefix() {
		this.environment.setProperty("spring.groovy.template.prefix", "prefix/");
		assertThat(this.provider.isTemplateAvailable("prefixed", this.environment, getClass().getClassLoader(),
				this.resourceLoader))
			.isTrue();
	}

	@Test
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
