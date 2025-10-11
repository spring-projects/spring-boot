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

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.boot.http.client.autoconfigure.reactive.ReactiveHttpClientAutoConfiguration;
import org.springframework.boot.http.client.autoconfigure.service.HttpServiceClientProperties;
import org.springframework.boot.http.client.reactive.ClientHttpConnectorBuilder;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.boot.webclient.WebClientCustomizer;
import org.springframework.boot.webclient.autoconfigure.WebClientAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ResourceLoader;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.registry.HttpServiceProxyRegistry;

/**
 * AutoConfiguration for Spring reactive HTTP Service Clients backed by {@link WebClient}.
 *
 * @author Olga Maciaszek-Sharma
 * @author Rossen Stoyanchev
 * @author Phillip Webb
 * @since 4.0.0
 */
@AutoConfiguration(after = { ReactiveHttpClientAutoConfiguration.class, WebClientAutoConfiguration.class })
@ConditionalOnClass(WebClientAdapter.class)
@ConditionalOnBean(HttpServiceProxyRegistry.class)
@EnableConfigurationProperties(HttpServiceClientProperties.class)
public final class ReactiveHttpServiceClientAutoConfiguration {

	@Bean
	PropertiesWebClientHttpServiceGroupConfigurer webClientPropertiesHttpServiceGroupConfigurer(
			ResourceLoader resourceLoader, HttpServiceClientProperties properties,
			ObjectProvider<SslBundles> sslBundles, ObjectProvider<ClientHttpConnectorBuilder<?>> clientConnectorBuilder,
			ObjectProvider<HttpClientSettings> httpClientSettings) {
		return new PropertiesWebClientHttpServiceGroupConfigurer(resourceLoader.getClassLoader(), properties,
				sslBundles.getIfAvailable(), clientConnectorBuilder, httpClientSettings.getIfAvailable());
	}

	@Bean
	WebClientCustomizerHttpServiceGroupConfigurer webClientCustomizerHttpServiceGroupConfigurer(
			ObjectProvider<WebClientCustomizer> customizers) {
		return new WebClientCustomizerHttpServiceGroupConfigurer(customizers);
	}

}
