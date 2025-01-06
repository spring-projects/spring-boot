/*
 * Copyright 2012-2024 the original author or authors.
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

import java.util.List;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletPath;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.reactive.WebTestClientAutoConfiguration;
import org.springframework.boot.test.web.reactive.server.WebTestClientBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.client.MockMvcWebTestClient;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * Auto-configuration for {@link MockMvc}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @author Brian Clozel
 * @since 1.4.0
 * @see AutoConfigureWebMvc
 */
@AutoConfiguration(after = { WebMvcAutoConfiguration.class, WebTestClientAutoConfiguration.class })
@ConditionalOnWebApplication(type = Type.SERVLET)
@EnableConfigurationProperties({ ServerProperties.class, WebMvcProperties.class })
@Import({ MockMvcConfiguration.class, MockMvcTesterConfiguration.class })
public class MockMvcAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public DispatcherServletPath dispatcherServletPath(WebMvcProperties webMvcProperties) {
		return () -> webMvcProperties.getServlet().getPath();
	}

	@Bean
	@ConditionalOnMissingBean
	public DispatcherServlet dispatcherServlet(MockMvc mockMvc) {
		return mockMvc.getDispatcherServlet();
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass({ WebClient.class, WebTestClient.class })
	static class WebTestClientMockMvcConfiguration {

		@Bean
		@ConditionalOnMissingBean
		WebTestClient webTestClient(MockMvc mockMvc, List<WebTestClientBuilderCustomizer> customizers) {
			WebTestClient.Builder builder = MockMvcWebTestClient.bindTo(mockMvc);
			for (WebTestClientBuilderCustomizer customizer : customizers) {
				customizer.customize(builder);
			}
			return builder.build();
		}

	}

}
