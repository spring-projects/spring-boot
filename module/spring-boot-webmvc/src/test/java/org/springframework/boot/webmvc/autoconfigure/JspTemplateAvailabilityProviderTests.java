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

package org.springframework.boot.webmvc.autoconfigure;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.boot.testsupport.classpath.resources.WithResource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.util.FileCopyUtils;

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

	@Test
	void availabilityOfTemplateInSrcMainWebapp(@TempDir File rootDirectory) throws Exception {
		File jsp = new File(rootDirectory, "src/main/webapp/test.jsp");
		jsp.getParentFile().mkdirs();
		FileCopyUtils.copy(new byte[0], jsp);
		Map<String, String> systemEnvironment = new HashMap<>();
		JspTemplateAvailabilityProvider provider = new JspTemplateAvailabilityProvider(rootDirectory,
				systemEnvironment::get);
		assertThat(isTemplateAvailable(provider, "test.jsp")).isTrue();
		assertThat(isTemplateAvailable(provider, "missing.jsp")).isFalse();
	}

	@Test
	void availabilityOfTemplateInCustomSrcMainWebapp(@TempDir File rootDirectory) throws Exception {
		File jsp = new File(rootDirectory, "src/main/unusual/test.jsp");
		jsp.getParentFile().mkdirs();
		FileCopyUtils.copy(new byte[0], jsp);
		Map<String, String> systemEnvironment = new HashMap<>();
		systemEnvironment.put("WAR_SOURCE_DIRECTORY", "src/main/unusual");
		JspTemplateAvailabilityProvider provider = new JspTemplateAvailabilityProvider(rootDirectory,
				systemEnvironment::get);
		assertThat(isTemplateAvailable(provider, "test.jsp")).isTrue();
		assertThat(isTemplateAvailable(provider, "missing.jsp")).isFalse();
	}

	private boolean isTemplateAvailable(String view) {
		return isTemplateAvailable(this.provider, view);
	}

	private boolean isTemplateAvailable(JspTemplateAvailabilityProvider provider, String view) {
		return provider.isTemplateAvailable(view, this.environment, getClass().getClassLoader(), this.resourceLoader);
	}

}
