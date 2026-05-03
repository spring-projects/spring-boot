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

package org.springframework.boot.http.client.autoconfigure.reactive;

import java.util.List;

import reactor.core.publisher.Mono;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.boot.http.client.autoconfigure.HttpClientAutoConfiguration;
import org.springframework.boot.http.client.reactive.ClientHttpConnectorBuilder;
import org.springframework.boot.http.client.reactive.JdkClientHttpConnectorBuilder;
import org.springframework.boot.http.client.reactive.ReactorClientHttpConnectorBuilder;
import org.springframework.boot.reactor.netty.autoconfigure.ReactorNettyConfigurations;
import org.springframework.boot.thread.Threading;
import org.springframework.boot.util.LambdaSafe;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.task.VirtualThreadTaskExecutor;
import org.springframework.http.client.ReactorResourceFactory;
import org.springframework.http.client.reactive.ClientHttpConnector;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for reactive HTTP clients.
 *
 * @author Phillip Webb
 * @since 4.0.0
 * @see HttpClientAutoConfiguration
 */
@AutoConfiguration(after = HttpClientAutoConfiguration.class)
@ConditionalOnClass({ ClientHttpConnector.class, Mono.class })
@Conditional(ConditionalOnClientHttpConnectorBuilderDetection.class)
@EnableConfigurationProperties(ReactiveHttpClientsProperties.class)
public final class ReactiveHttpClientAutoConfiguration {

	private final Environment environment;

	ReactiveHttpClientAutoConfiguration(Environment environment) {
		this.environment = environment;
	}

	@Bean
	@ConditionalOnMissingBean
	ClientHttpConnectorBuilder<?> clientHttpConnectorBuilder(ResourceLoader resourceLoader,
			ReactiveHttpClientsProperties properties,
			ObjectProvider<ClientHttpConnectorBuilderCustomizer<?>> clientHttpConnectorBuilderCustomizers) {
		ClientHttpConnectorBuilder<?> builder = (properties.getConnector() != null)
				? properties.getConnector().builder()
				: ClientHttpConnectorBuilder.detect(resourceLoader.getClassLoader());
		if (builder instanceof JdkClientHttpConnectorBuilder jdk && Threading.VIRTUAL.isActive(this.environment)) {
			builder = jdk.withExecutor(new VirtualThreadTaskExecutor("httpclient-"));
		}
		return customize(builder, clientHttpConnectorBuilderCustomizers.orderedStream().toList());
	}

	@SuppressWarnings("unchecked")
	private ClientHttpConnectorBuilder<?> customize(ClientHttpConnectorBuilder<?> builder,
			List<ClientHttpConnectorBuilderCustomizer<?>> customizers) {
		ClientHttpConnectorBuilder<?>[] builderReference = { builder };
		LambdaSafe.callbacks(ClientHttpConnectorBuilderCustomizer.class, customizers, builderReference[0])
			.invoke((customizer) -> builderReference[0] = customizer.customize(builderReference[0]));
		return builderReference[0];
	}

	@Bean
	@Lazy
	@ConditionalOnMissingBean
	ClientHttpConnector clientHttpConnector(ResourceLoader resourceLoader,
			ObjectProvider<ClientHttpConnectorBuilder<?>> clientHttpConnectorBuilder,
			ObjectProvider<HttpClientSettings> httpClientSettings) {
		return clientHttpConnectorBuilder
			.getIfAvailable(() -> ClientHttpConnectorBuilder.detect(resourceLoader.getClassLoader()))
			.build(httpClientSettings.getIfAvailable(HttpClientSettings::defaults));
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass({ reactor.netty.http.client.HttpClient.class, ReactorNettyConfigurations.class })
	@Import(ReactorNettyConfigurations.ReactorResourceFactoryConfiguration.class)
	static class ReactorNetty {

		@Bean
		@Order(0)
		ClientHttpConnectorBuilderCustomizer<ReactorClientHttpConnectorBuilder> reactorResourceFactoryClientHttpConnectorBuilderCustomizer(
				ReactorResourceFactory reactorResourceFactory) {
			return (builder) -> builder.withReactorResourceFactory(reactorResourceFactory);
		}

	}

}
