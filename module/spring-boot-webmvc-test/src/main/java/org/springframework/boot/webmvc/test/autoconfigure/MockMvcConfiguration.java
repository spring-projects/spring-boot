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

package org.springframework.boot.webmvc.test.autoconfigure;

import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.webmvc.autoconfigure.WebMvcProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.web.servlet.DispatcherServletCustomizer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MockMvcBuilder;
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * Configuration for core {@link MockMvc}.
 *
 * @author Stephane Nicoll
 */
@Configuration(proxyBeanMethods = false)
class MockMvcConfiguration {

	private final WebApplicationContext context;

	private final WebMvcProperties webMvcProperties;

	MockMvcConfiguration(WebApplicationContext context, WebMvcProperties webMvcProperties) {
		this.context = context;
		this.webMvcProperties = webMvcProperties;
	}

	@Bean
	@ConditionalOnMissingBean(MockMvcBuilder.class)
	DefaultMockMvcBuilder mockMvcBuilder(List<MockMvcBuilderCustomizer> customizers) {
		DefaultMockMvcBuilder builder = MockMvcBuilders.webAppContextSetup(this.context);
		builder.addDispatcherServletCustomizer(new MockMvcDispatcherServletCustomizer(this.webMvcProperties));
		for (MockMvcBuilderCustomizer customizer : customizers) {
			customizer.customize(builder);
		}
		return builder;
	}

	@Bean
	@ConfigurationProperties("spring.test.mockmvc")
	SpringBootMockMvcBuilderCustomizer springBootMockMvcBuilderCustomizer() {
		return new SpringBootMockMvcBuilderCustomizer(this.context);
	}

	@Bean
	@ConditionalOnMissingBean
	MockMvc mockMvc(MockMvcBuilder builder) {
		return builder.build();
	}

	private static class MockMvcDispatcherServletCustomizer implements DispatcherServletCustomizer {

		private final WebMvcProperties webMvcProperties;

		MockMvcDispatcherServletCustomizer(WebMvcProperties webMvcProperties) {
			this.webMvcProperties = webMvcProperties;
		}

		@Override
		public void customize(DispatcherServlet dispatcherServlet) {
			dispatcherServlet.setDispatchOptionsRequest(this.webMvcProperties.isDispatchOptionsRequest());
			dispatcherServlet.setDispatchTraceRequest(this.webMvcProperties.isDispatchTraceRequest());
		}

	}

}
