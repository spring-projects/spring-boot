/*
 * Copyright 2012-2019 the original author or authors.
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

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.web.reactive.function.client.WebClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
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
 * @since 2.1.0
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(WebClient.class)
@Import({ ClientHttpConnectorConfiguration.ReactorNetty.class, ClientHttpConnectorConfiguration.JettyClient.class })
public class ClientHttpConnectorAutoConfiguration {

	@Bean
	@Order(0)
	@ConditionalOnBean(ClientHttpConnector.class)
	public WebClientCustomizer clientConnectorCustomizer(ClientHttpConnector clientHttpConnector) {
		return (builder) -> builder.clientConnector(clientHttpConnector);
	}

}
