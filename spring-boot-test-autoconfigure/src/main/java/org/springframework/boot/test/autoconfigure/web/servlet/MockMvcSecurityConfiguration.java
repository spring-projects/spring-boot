/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.test.autoconfigure.web.servlet;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.test.context.TestSecurityContextHolder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.security.web.context.AbstractSecurityWebApplicationInitializer;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.ConfigurableMockMvcBuilder;
import org.springframework.test.web.servlet.setup.MockMvcConfigurerAdapter;
import org.springframework.web.context.WebApplicationContext;

/**
 * Configuration for Spring Security's MockMvc integration.
 *
 * @author Andy Wilkinson
 */
@Configuration
@ConditionalOnClass(SecurityMockMvcRequestPostProcessors.class)
class MockMvcSecurityConfiguration {

	private static final String DEFAULT_SECURITY_FILTER_NAME = AbstractSecurityWebApplicationInitializer.DEFAULT_FILTER_NAME;

	@Bean
	@ConditionalOnBean(name = DEFAULT_SECURITY_FILTER_NAME)
	public SecurityMockMvcBuilderCustomizer securityMockMvcBuilderCustomizer() {
		return new SecurityMockMvcBuilderCustomizer();
	}

	/**
	 * {@link MockMvcBuilderCustomizer} that ensures that requests run with the user in
	 * the {@link TestSecurityContextHolder}.
	 *
	 * @see SecurityMockMvcRequestPostProcessors#testSecurityContext
	 */
	class SecurityMockMvcBuilderCustomizer implements MockMvcBuilderCustomizer {

		@Override
		public void customize(ConfigurableMockMvcBuilder<?> builder) {
			builder.apply(new MockMvcConfigurerAdapter() {

				@Override
				public RequestPostProcessor beforeMockMvcCreated(
						ConfigurableMockMvcBuilder<?> builder,
						WebApplicationContext context) {
					return SecurityMockMvcRequestPostProcessors.testSecurityContext();
				}

			});
		}

	}

}
