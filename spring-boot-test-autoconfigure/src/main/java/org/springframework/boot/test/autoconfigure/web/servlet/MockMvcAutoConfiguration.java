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

import java.util.List;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MockMvcBuilder;
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Auto-configuration for {@link MockMvc}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @see AutoConfigureWebMvc
 * @since 1.4.0
 */
@Configuration
@ConditionalOnWebApplication
@AutoConfigureAfter(WebMvcAutoConfiguration.class)
@EnableConfigurationProperties
public class MockMvcAutoConfiguration {

	private final WebApplicationContext context;

	MockMvcAutoConfiguration(WebApplicationContext context) {
		this.context = context;
	}

	@Bean
	@ConditionalOnMissingBean(MockMvcBuilder.class)
	public DefaultMockMvcBuilder mockMvcBuilder(
			List<MockMvcBuilderCustomizer> customizers) {
		DefaultMockMvcBuilder builder = MockMvcBuilders.webAppContextSetup(this.context);
		for (MockMvcBuilderCustomizer customizer : customizers) {
			customizer.customize(builder);
		}
		return builder;
	}

	@Bean
	@ConfigurationProperties("spring.test.mockmvc")
	public SpringBootMockMvcBuilderCustomizer springBootMockMvcBuilderCustomizer() {
		return new SpringBootMockMvcBuilderCustomizer(this.context);
	}

	@Bean
	@ConditionalOnMissingBean
	public MockMvc mockMvc(MockMvcBuilder builder) {
		return builder.build();
	}

}
