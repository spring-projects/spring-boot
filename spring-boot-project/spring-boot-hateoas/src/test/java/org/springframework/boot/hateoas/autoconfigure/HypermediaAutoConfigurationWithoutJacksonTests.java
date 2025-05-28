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

package org.springframework.boot.hateoas.autoconfigure;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.testsupport.classpath.ClassPathExclusions;
import org.springframework.boot.web.context.servlet.AnnotationConfigServletWebApplicationContext;
import org.springframework.mock.web.MockServletContext;

/**
 * Tests for {@link HypermediaAutoConfiguration} when Jackson is not on the classpath.
 *
 * @author Andy Wilkinson
 */
@ClassPathExclusions("jackson-*.jar")
class HypermediaAutoConfigurationWithoutJacksonTests {

	private AnnotationConfigServletWebApplicationContext context;

	@Test
	void jacksonRelatedConfigurationBacksOff() {
		this.context = new AnnotationConfigServletWebApplicationContext();
		this.context.register(BaseConfig.class);
		this.context.setServletContext(new MockServletContext());
		this.context.refresh();
	}

	@ImportAutoConfiguration(HypermediaAutoConfiguration.class)
	static class BaseConfig {

	}

}
