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

package org.springframework.boot.webtestclient;

import java.util.List;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.http.codec.CodecCustomizer;
import org.springframework.boot.http.codec.autoconfigure.CodecsAutoConfiguration;
import org.springframework.boot.test.http.server.LocalTestWebServer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.test.web.reactive.server.MockServerConfigurer;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.reactive.server.WebTestClient.MockServerSpec;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.client.MockMvcWebTestClient;
import org.springframework.util.ClassUtils;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.adapter.WebHttpHandlerBuilder;

/**
 * Auto-configuration for {@link WebTestClient}.
 *
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @since 4.0.0
 */
@AutoConfiguration(after = CodecsAutoConfiguration.class)
@ConditionalOnClass({ CodecCustomizer.class, WebClient.class, WebTestClient.class })
@EnableConfigurationProperties
public final class WebTestClientAutoConfiguration {

	private static final String WEB_APPLICATION_CONTEXT_CLASS = "org.springframework.web.context.WebApplicationContext";

	@Bean
	@ConfigurationProperties("spring.test.webtestclient")
	SpringBootWebTestClientBuilderCustomizer springBootWebTestClientBuilderCustomizer(
			ObjectProvider<CodecCustomizer> codecCustomizers) {
		return new SpringBootWebTestClientBuilderCustomizer(codecCustomizers.orderedStream().toList());
	}

	@Bean
	@ConditionalOnMissingBean
	WebTestClient webTestClient(ApplicationContext applicationContext, List<WebTestClientBuilderCustomizer> customizers,
			List<MockServerConfigurer> configurers) {
		WebTestClient.Builder builder = getBuilder(applicationContext, configurers);
		customizers.forEach((customizer) -> customizer.customize(builder));
		return builder.build();
	}

	private WebTestClient.Builder getBuilder(ApplicationContext applicationContext,
			List<MockServerConfigurer> configurers) {
		LocalTestWebServer localTestWebServer = LocalTestWebServer.get(applicationContext);
		if (localTestWebServer != null) {
			return WebTestClient.bindToServer().uriBuilderFactory(localTestWebServer.uriBuilderFactory());
		}
		if (applicationContext.containsBean(WebHttpHandlerBuilder.WEB_HANDLER_BEAN_NAME)) {
			MockServerSpec<?> spec = WebTestClient.bindToApplicationContext(applicationContext);
			configurers.forEach(spec::apply);
			return spec.configureClient();
		}
		if (ClassUtils.isPresent(WEB_APPLICATION_CONTEXT_CLASS, applicationContext.getClassLoader())) {
			MockMvc mockMvc = applicationContext.getBeanProvider(MockMvc.class).getIfUnique();
			if (mockMvc != null) {
				return MockMvcWebTestClient.bindTo(mockMvc);
			}
			if (applicationContext instanceof WebApplicationContext webApplicationContext) {
				return MockMvcWebTestClient.bindToApplicationContext(webApplicationContext).configureClient();
			}
		}
		throw new IllegalStateException(
				"Mock WebTestClient support requires a WebHandler (named 'webHandler') bean or a WebApplicationContext and neither was present");
	}

}
