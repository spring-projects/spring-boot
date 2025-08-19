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

package org.springframework.boot.webclient.autoconfigure.service;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.http.client.reactive.ClientHttpConnectorBuilder;
import org.springframework.boot.http.client.reactive.ClientHttpConnectorSettings;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.boot.webclient.WebClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.ApiVersionFormatter;
import org.springframework.web.client.ApiVersionInserter;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientHttpServiceGroupConfigurer;
import org.springframework.web.service.registry.HttpServiceProxyRegistry;

/**
 * {@link Configuration @Configuration} to register
 * {@link WebClientHttpServiceGroupConfigurer} beans to support HTTP service clients
 * backed by a {@link WebClient}.
 *
 * @author Phillip Webb
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnBean(HttpServiceProxyRegistry.class)
final class WebClientHttpServiceClientConfiguration implements BeanClassLoaderAware {

	@SuppressWarnings("NullAway.Init")
	private ClassLoader beanClassLoader;

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.beanClassLoader = classLoader;
	}

	@Bean
	WebClientPropertiesHttpServiceGroupConfigurer webClientPropertiesHttpServiceGroupConfigurer(
			ObjectProvider<SslBundles> sslBundles, ReactiveHttpClientServiceProperties serviceProperties,
			ObjectProvider<ClientHttpConnectorBuilder<?>> clientConnectorBuilder,
			ObjectProvider<ClientHttpConnectorSettings> clientConnectorSettings,
			ObjectProvider<ApiVersionInserter> apiVersionInserter,
			ObjectProvider<ApiVersionFormatter> apiVersionFormatter) {
		return new WebClientPropertiesHttpServiceGroupConfigurer(this.beanClassLoader, sslBundles, serviceProperties,
				clientConnectorBuilder, clientConnectorSettings, apiVersionInserter, apiVersionFormatter);
	}

	@Bean
	WebClientCustomizerHttpServiceGroupConfigurer webClientCustomizerHttpServiceGroupConfigurer(
			ObjectProvider<WebClientCustomizer> customizers) {
		return new WebClientCustomizerHttpServiceGroupConfigurer(customizers);
	}

}
