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

package org.springframework.boot.restclient.autoconfigure.service;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.boot.http.client.autoconfigure.blocking.BlockingHttpClientAutoConfiguration;
import org.springframework.boot.http.client.autoconfigure.service.HttpServiceClientProperties;
import org.springframework.boot.restclient.RestClientCustomizer;
import org.springframework.boot.restclient.autoconfigure.RestClientAutoConfiguration;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.core.io.ResourceLoader;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.registry.HttpServiceProxyRegistry;

/**
 * AutoConfiguration for Spring HTTP Service clients backed by {@link RestClient}.
 *
 * @author Olga Maciaszek-Sharma
 * @author Rossen Stoyanchev
 * @author Phillip Webb
 * @since 4.0.0
 */
@AutoConfiguration(after = { BlockingHttpClientAutoConfiguration.class, RestClientAutoConfiguration.class })
@ConditionalOnClass(RestClientAdapter.class)
@ConditionalOnBean(HttpServiceProxyRegistry.class)
@Conditional(NotReactiveWebApplicationCondition.class)
@EnableConfigurationProperties(HttpServiceClientProperties.class)
public final class HttpServiceClientAutoConfiguration {

	@Bean
	PropertiesRestClientHttpServiceGroupConfigurer restClientPropertiesHttpServiceGroupConfigurer(
			ResourceLoader resourceLoader, HttpServiceClientProperties properties,
			ObjectProvider<SslBundles> sslBundles,
			ObjectProvider<ClientHttpRequestFactoryBuilder<?>> requestFactoryBuilder,
			ObjectProvider<HttpClientSettings> httpClientSettings) {
		return new PropertiesRestClientHttpServiceGroupConfigurer(resourceLoader.getClassLoader(), properties,
				sslBundles.getIfAvailable(), requestFactoryBuilder, httpClientSettings.getIfAvailable());
	}

	@Bean
	RestClientCustomizerHttpServiceGroupConfigurer restClientCustomizerHttpServiceGroupConfigurer(
			ObjectProvider<RestClientCustomizer> customizers) {
		return new RestClientCustomizerHttpServiceGroupConfigurer(customizers);
	}

}
