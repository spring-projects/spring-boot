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

package org.springframework.boot.test.autoconfigure.web.reactive;

import java.util.List;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.http.codec.CodecCustomizer;
import org.springframework.boot.web.server.test.client.reactive.WebTestClientBuilderCustomizer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.reactive.server.MockServerConfigurer;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.WebHandler;

/**
 * Auto-configuration for {@link WebTestClient}.
 *
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 * @since 2.0.0
 */
@AutoConfiguration(afterName = { "org.springframework.boot.http.codec.autoconfigure.CodecsAutoConfiguration",
		"org.springframework.boot.webflux.autoconfigure.WebFluxAutoConfiguration" })
@ConditionalOnClass({ WebClient.class, WebTestClient.class })
@Import(WebTestClientSecurityConfiguration.class)
@EnableConfigurationProperties
public class WebTestClientAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnBean(WebHandler.class)
	public WebTestClient webTestClient(ApplicationContext applicationContext,
			List<WebTestClientBuilderCustomizer> customizers, List<MockServerConfigurer> configurers) {
		WebTestClient.MockServerSpec<?> mockServerSpec = WebTestClient.bindToApplicationContext(applicationContext);
		for (MockServerConfigurer configurer : configurers) {
			mockServerSpec.apply(configurer);
		}
		WebTestClient.Builder builder = mockServerSpec.configureClient();
		for (WebTestClientBuilderCustomizer customizer : customizers) {
			customizer.customize(builder);
		}
		return builder.build();
	}

	@Bean
	@ConfigurationProperties("spring.test.webtestclient")
	public SpringBootWebTestClientBuilderCustomizer springBootWebTestClientBuilderCustomizer(
			ObjectProvider<CodecCustomizer> codecCustomizers) {
		return new SpringBootWebTestClientBuilderCustomizer(codecCustomizers.orderedStream().toList());
	}

}
