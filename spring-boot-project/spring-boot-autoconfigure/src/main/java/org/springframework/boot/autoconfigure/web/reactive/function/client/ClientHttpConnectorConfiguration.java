/*
 * Copyright 2012-2020 the original author or authors.
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

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.JettyClientHttpConnector;
import org.springframework.http.client.reactive.JettyResourceFactory;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.client.reactive.ReactorResourceFactory;

/**
 * Configuration classes for WebClient client connectors.
 * <p>
 * Those should be {@code @Import} in a regular auto-configuration class to guarantee
 * their order of execution.
 *
 * @author Brian Clozel
 */
@Configuration(proxyBeanMethods = false)
class ClientHttpConnectorConfiguration {

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(reactor.netty.http.client.HttpClient.class)
	@ConditionalOnMissingBean(ClientHttpConnector.class)
	public static class ReactorNetty {

		@Bean
		@ConditionalOnMissingBean
		public ReactorResourceFactory reactorClientResourceFactory() {
			return new ReactorResourceFactory();
		}

		@Bean
		@Lazy
		public ReactorClientHttpConnector reactorClientHttpConnector(ReactorResourceFactory reactorResourceFactory,
				ObjectProvider<ReactorNettyHttpClientMapper> mapperProvider) {
			ReactorNettyHttpClientMapper mapper = mapperProvider.orderedStream()
					.reduce((before, after) -> (client) -> after.configure(before.configure(client)))
					.orElse((client) -> client);
			return new ReactorClientHttpConnector(reactorResourceFactory, mapper::configure);
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(org.eclipse.jetty.reactive.client.ReactiveRequest.class)
	@ConditionalOnMissingBean(ClientHttpConnector.class)
	public static class JettyClient {

		@Bean
		@ConditionalOnMissingBean
		public JettyResourceFactory jettyClientResourceFactory() {
			return new JettyResourceFactory();
		}

		@Bean
		@Lazy
		public JettyClientHttpConnector jettyClientHttpConnector(JettyResourceFactory jettyResourceFactory) {
			SslContextFactory sslContextFactory = new SslContextFactory.Client();
			HttpClient httpClient = new HttpClient(sslContextFactory);
			return new JettyClientHttpConnector(httpClient, jettyResourceFactory);
		}

	}

}
