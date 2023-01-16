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

import org.apache.hc.client5.http.impl.async.HttpAsyncClients;
import org.apache.hc.core5.http.nio.AsyncRequestProducer;
import org.apache.hc.core5.reactive.ReactiveResponseConsumer;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.reactor.netty.ReactorNettyConfigurations;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.client.reactive.ReactorResourceFactory;

/**
 * Configuration classes for WebClient client connectors.
 * <p>
 * Those should be {@code @Import} in a regular auto-configuration class to guarantee
 * their order of execution.
 *
 * @author Brian Clozel
 */
class ClientHttpConnectorFactoryConfiguration {

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(reactor.netty.http.client.HttpClient.class)
	@ConditionalOnMissingBean(ClientHttpConnectorFactory.class)
	@Import(ReactorNettyConfigurations.ReactorResourceFactoryConfiguration.class)
	static class ReactorNetty {

		@Bean
		ReactorClientHttpConnectorFactory reactorClientHttpConnectorFactory(
				ReactorResourceFactory reactorResourceFactory,
				ObjectProvider<ReactorNettyHttpClientMapper> mapperProvider) {
			return new ReactorClientHttpConnectorFactory(reactorResourceFactory, mapperProvider::orderedStream);
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass({ HttpAsyncClients.class, AsyncRequestProducer.class, ReactiveResponseConsumer.class })
	@ConditionalOnMissingBean(ClientHttpConnectorFactory.class)
	static class HttpClient5 {

		@Bean
		HttpComponentsClientHttpConnectorFactory httpComponentsClientHttpConnectorFactory() {
			return new HttpComponentsClientHttpConnectorFactory();
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(java.net.http.HttpClient.class)
	@ConditionalOnMissingBean(ClientHttpConnectorFactory.class)
	static class JdkClient {

		@Bean
		JdkClientHttpConnectorFactory jdkClientHttpConnectorFactory() {
			return new JdkClientHttpConnectorFactory();
		}

	}

}
