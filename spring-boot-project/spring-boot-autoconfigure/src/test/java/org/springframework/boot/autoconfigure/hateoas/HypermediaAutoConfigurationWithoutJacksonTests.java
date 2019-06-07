/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.autoconfigure.hateoas;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.testsupport.runner.classpath.ClassPathExclusions;
import org.springframework.boot.testsupport.runner.classpath.ModifiedClassPathRunner;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

/**
 * Tests for {@link HypermediaAutoConfiguration} when Jackson is not on the classpath.
 *
 * @author Andy Wilkinson
 */
@RunWith(ModifiedClassPathRunner.class)
@ClassPathExclusions("jackson-*.jar")
public class HypermediaAutoConfigurationWithoutJacksonTests {

	private AnnotationConfigWebApplicationContext context;

	@Test
	public void jacksonRelatedConfigurationBacksOff() {
		this.context = new AnnotationConfigWebApplicationContext();
		this.context.register(BaseConfig.class);
		this.context.setServletContext(new MockServletContext());
		this.context.refresh();
	}

	@ImportAutoConfiguration({ HttpMessageConvertersAutoConfiguration.class, WebMvcAutoConfiguration.class,
			HypermediaAutoConfiguration.class })
	static class BaseConfig {

	}

}
