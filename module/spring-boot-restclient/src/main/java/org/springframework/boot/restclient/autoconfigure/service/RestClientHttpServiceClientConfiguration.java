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

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.boot.restclient.RestClientCustomizer;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.ApiVersionFormatter;
import org.springframework.web.client.ApiVersionInserter;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientHttpServiceGroupConfigurer;
import org.springframework.web.service.registry.HttpServiceProxyRegistry;

/**
 * {@link Configuration @Configuration} to register
 * {@link RestClientHttpServiceGroupConfigurer} beans to support HTTP service clients
 * backed by a {@link RestClient}.
 *
 * @author Phillip Webb
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnBean(HttpServiceProxyRegistry.class)
class RestClientHttpServiceClientConfiguration implements BeanClassLoaderAware {

	@SuppressWarnings("NullAway.Init")
	private ClassLoader beanClassLoader;

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.beanClassLoader = classLoader;
	}

	@Bean
	RestClientPropertiesHttpServiceGroupConfigurer restClientPropertiesHttpServiceGroupConfigurer(
			ObjectProvider<SslBundles> sslBundles, HttpClientServiceProperties serviceProperties,
			ObjectProvider<ClientHttpRequestFactoryBuilder<?>> clientFactoryBuilder,
			ObjectProvider<ClientHttpRequestFactorySettings> clientHttpRequestFactorySettings,
			ObjectProvider<ApiVersionInserter> apiVersionInserter,
			ObjectProvider<ApiVersionFormatter> apiVersionFormatter) {
		return new RestClientPropertiesHttpServiceGroupConfigurer(this.beanClassLoader, sslBundles, serviceProperties,
				clientFactoryBuilder, clientHttpRequestFactorySettings, apiVersionInserter, apiVersionFormatter);
	}

	@Bean
	RestClientCustomizerHttpServiceGroupConfigurer restClientCustomizerHttpServiceGroupConfigurer(
			ObjectProvider<RestClientCustomizer> customizers) {
		return new RestClientCustomizerHttpServiceGroupConfigurer(customizers);
	}

}
