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

package org.springframework.boot.web.servlet;

import jakarta.servlet.Registration.Dynamic;
import jakarta.servlet.ServletContext;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DynamicRegistrationBean}.
 *
 * @author Moritz Halbritter
 */
class DynamicRegistrationBeanTests {

	@Test
	void shouldUseNameIfSet() {
		DynamicRegistrationBean<?> bean = createBean();
		bean.setName("givenName");
		assertThat(bean.getOrDeduceName("dummy")).isEqualTo("givenName");
	}

	@Test
	void shouldUseBeanNameIfNameIsNotSet() {
		DynamicRegistrationBean<?> bean = createBean();
		bean.setBeanName("beanName");
		assertThat(bean.getOrDeduceName("dummy")).isEqualTo("beanName");
	}

	@Test
	void shouldUseConventionBasedNameIfNoNameOrBeanNameIsSet() {
		DynamicRegistrationBean<?> bean = createBean();
		assertThat(bean.getOrDeduceName("dummy")).isEqualTo("string");
	}

	private static DynamicRegistrationBean<?> createBean() {
		return new DynamicRegistrationBean<Dynamic>() {
			@Override
			protected Dynamic addRegistration(String description, ServletContext servletContext) {
				return null;
			}

			@Override
			protected String getDescription() {
				return null;
			}
		};
	}

}
