/*
 * Copyright 2012-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.autoconfigure.web;

import org.junit.Test;
import org.springframework.boot.autoconfigure.template.TemplateAvailabilityProvider;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests for {@link JspTemplateAvailabilityProvider}.
 *
 * @author Yunkun Huang
 */
public class JspTemplateAvailabilityProviderTest {
	private final TemplateAvailabilityProvider provider = new JspTemplateAvailabilityProvider();

	private final ResourceLoader resourceLoader = new DefaultResourceLoader();

	private final MockEnvironment environment = new MockEnvironment();

	@Test
	public void availabilityOfTemplateThatDoesNotExist() {
		assertFalse(this.provider.isTemplateAvailable("whatever", this.environment,
				getClass().getClassLoader(), this.resourceLoader));
	}

	@Test
	public void availabilityOfTemplateWithCustomPrefix() {
		this.environment.setProperty("spring.mvc.view.prefix",
				"classpath:/custom-templates/");

		assertTrue(this.provider.isTemplateAvailable("custom.jsp", this.environment,
				getClass().getClassLoader(), this.resourceLoader));
	}

	@Test
	public void availabilityOfTemplateWithCustomSuffix() {
		this.environment.setProperty("spring.mvc.view.prefix", "classpath:/custom-templates/");
		this.environment.setProperty("spring.mvc.view.suffix", ".jsp");

		assertTrue(this.provider.isTemplateAvailable("suffixed", this.environment,
				getClass().getClassLoader(), this.resourceLoader));
	}

	@Test
	public void notAvailabilityOfTemplateWithCustomSuffixViaDeprecatedKey() {
		this.environment.setProperty("spring.mvc.view.prefix", "classpath:/custom-templates/");
		this.environment.setProperty("spring.view.suffix", ".jsp");

		assertFalse(this.provider.isTemplateAvailable("suffixed", this.environment,
				getClass().getClassLoader(), this.resourceLoader));
	}
}
