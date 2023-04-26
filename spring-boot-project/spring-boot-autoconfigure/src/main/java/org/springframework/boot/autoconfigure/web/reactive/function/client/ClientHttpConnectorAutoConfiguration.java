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

package org.springframework.boot.autoconfigure.web.reactive.function.client;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.ssl.SslAutoConfiguration;
import org.springframework.boot.web.reactive.function.client.WebClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.annotation.Order;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link ClientHttpConnector}.
 * <p>
 * It can produce a {@link org.springframework.http.client.reactive.ClientHttpConnector}
 * bean and possibly a companion {@code ResourceFactory} bean, depending on the chosen
 * HTTP client library.
 *
 * @author Brian Clozel
 * @author Phillip Webb
 * @since 2.1.0
 */
@AutoConfiguration
@ConditionalOnClass(WebClient.class)
@AutoConfigureAfter(SslAutoConfiguration.class)
@Import({ ClientHttpConnectorFactoryConfiguration.ReactorNetty.class,
		ClientHttpConnectorFactoryConfiguration.JettyClient.class,
		ClientHttpConnectorFactoryConfiguration.HttpClient5.class,
		ClientHttpConnectorFactoryConfiguration.JdkClient.class })
public class ClientHttpConnectorAutoConfiguration {

	@Bean
	@Lazy
	@ConditionalOnMissingBean(ClientHttpConnector.class)
	ClientHttpConnector webClientHttpConnector(ClientHttpConnectorFactory<?> clientHttpConnectorFactory) {
		return clientHttpConnectorFactory.createClientHttpConnector();
	}

	@Bean
	@Lazy
	@Order(0)
	@ConditionalOnBean(ClientHttpConnector.class)
	public WebClientCustomizer webClientHttpConnectorCustomizer(ClientHttpConnector clientHttpConnector) {
		return (builder) -> builder.clientConnector(clientHttpConnector);
	}

}
