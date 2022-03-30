/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.autoconfigure.mustache;

import com.samskivert.mustache.Mustache;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.testsupport.classpath.ClassPathExclusions;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MustacheAutoConfiguration} without Spring MVC on the class path.
 *
 * @author Andy Wilkinson
 */
@ClassPathExclusions("spring-webmvc-*.jar")
class MustacheAutoConfigurationWithoutWebMvcTests {

	@Test
	void registerBeansForServletAppWithoutMvc() {
		new WebApplicationContextRunner().withConfiguration(AutoConfigurations.of(MustacheAutoConfiguration.class))
				.run((context) -> {
					assertThat(context).hasSingleBean(Mustache.Compiler.class);
					assertThat(context).hasSingleBean(MustacheResourceTemplateLoader.class);
				});
	}

}
