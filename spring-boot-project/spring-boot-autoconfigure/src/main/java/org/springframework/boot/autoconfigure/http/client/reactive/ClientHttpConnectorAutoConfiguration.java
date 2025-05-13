/*
 * Copyright 2012-2025 the original author or authors.
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

package org.springframework.boot.autoconfigure.http.client.reactive;

import java.util.List;

import reactor.core.publisher.Mono;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.reactor.netty.ReactorNettyConfigurations;
import org.springframework.boot.autoconfigure.ssl.SslAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.http.client.reactive.ClientHttpConnectorBuilder;
import org.springframework.boot.http.client.reactive.ClientHttpConnectorSettings;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.boot.util.LambdaSafe;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.client.reactive.ClientHttpConnector;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for
 * {@link ClientHttpConnectorBuilder} and {@link ClientHttpConnectorSettings}.
 *
 * @author Phillip Webb
 * @since 3.5.0
 */
@AutoConfiguration(after = SslAutoConfiguration.class)
@ConditionalOnClass({ ClientHttpConnector.class, Mono.class })
@EnableConfigurationProperties(HttpReactiveClientProperties.class)
public class ClientHttpConnectorAutoConfiguration implements BeanClassLoaderAware {

	private final ClientHttpConnectors connectors;

	private ClassLoader beanClassLoader;

	ClientHttpConnectorAutoConfiguration(ObjectProvider<SslBundles> sslBundles,
			HttpReactiveClientProperties properties) {
		this.connectors = new ClientHttpConnectors(sslBundles, properties);
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.beanClassLoader = classLoader;
	}

	@Bean
	@ConditionalOnMissingBean
	ClientHttpConnectorBuilder<?> clientHttpConnectorBuilder(
			ObjectProvider<ClientHttpConnectorBuilderCustomizer<?>> clientHttpConnectorBuilderCustomizers) {
		ClientHttpConnectorBuilder<?> builder = this.connectors.builder(this.beanClassLoader);
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
	@ConditionalOnMissingBean
	ClientHttpConnectorSettings clientHttpConnectorSettings() {
		return this.connectors.settings();
	}

	@Bean
	@Lazy
	@ConditionalOnMissingBean
	ClientHttpConnector clientHttpConnector(ClientHttpConnectorBuilder<?> clientHttpConnectorBuilder,
			ClientHttpConnectorSettings clientHttpRequestFactorySettings) {
		return clientHttpConnectorBuilder.build(clientHttpRequestFactorySettings);
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(reactor.netty.http.client.HttpClient.class)
	@Import(ReactorNettyConfigurations.ReactorResourceFactoryConfiguration.class)
	static class ReactorNetty {

	}

}
