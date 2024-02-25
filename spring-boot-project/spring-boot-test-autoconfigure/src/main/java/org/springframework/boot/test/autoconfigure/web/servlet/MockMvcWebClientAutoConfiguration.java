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

package org.springframework.boot.test.autoconfigure.web.servlet;

import com.gargoylesoftware.htmlunit.WebClient;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.test.web.htmlunit.LocalHostWebClient;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.htmlunit.MockMvcWebClientBuilder;

/**
 * Auto-configuration for HtmlUnit {@link WebClient} MockMVC integration.
 *
 * @author Phillip Webb
 * @since 1.4.0
 */
@AutoConfiguration(after = MockMvcAutoConfiguration.class)
@ConditionalOnClass(WebClient.class)
@ConditionalOnProperty(prefix = "spring.test.mockmvc.webclient", name = "enabled", matchIfMissing = true)
public class MockMvcWebClientAutoConfiguration {

	/**
	 * Creates a {@link MockMvcWebClientBuilder} bean if there is no existing bean of type
	 * {@link WebClient} or {@link MockMvcWebClientBuilder}, and if there is a bean of
	 * type {@link MockMvc}.
	 *
	 * This bean is conditional on the absence of beans of type {@link WebClient} and
	 * {@link MockMvcWebClientBuilder}, and the presence of a bean of type
	 * {@link MockMvc}.
	 *
	 * The created {@link MockMvcWebClientBuilder} bean is configured with the provided
	 * {@link MockMvc} instance, and a {@link LocalHostWebClient} delegate that uses the
	 * provided {@link Environment}.
	 * @param mockMvc the {@link MockMvc} instance to be used for configuring the
	 * {@link MockMvcWebClientBuilder}
	 * @param environment the {@link Environment} instance to be used by the
	 * {@link LocalHostWebClient} delegate
	 * @return the created {@link MockMvcWebClientBuilder} bean
	 */
	@Bean
	@ConditionalOnMissingBean({ WebClient.class, MockMvcWebClientBuilder.class })
	@ConditionalOnBean(MockMvc.class)
	public MockMvcWebClientBuilder mockMvcWebClientBuilder(MockMvc mockMvc, Environment environment) {
		return MockMvcWebClientBuilder.mockMvcSetup(mockMvc).withDelegate(new LocalHostWebClient(environment));
	}

	/**
	 * Creates a WebClient bean using HtmlUnit implementation if no other WebClient bean
	 * is present and if a MockMvcWebClientBuilder bean is present.
	 * @param builder the MockMvcWebClientBuilder used to build the WebClient
	 * @return the WebClient bean
	 */
	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnBean(MockMvcWebClientBuilder.class)
	public WebClient htmlUnitWebClient(MockMvcWebClientBuilder builder) {
		return builder.build();
	}

}
