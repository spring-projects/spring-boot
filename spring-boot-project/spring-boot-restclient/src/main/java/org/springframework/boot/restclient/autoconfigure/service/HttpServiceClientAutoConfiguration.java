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
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.boot.http.client.autoconfigure.HttpClientAutoConfiguration;
import org.springframework.boot.http.client.autoconfigure.HttpClientProperties;
import org.springframework.boot.restclient.RestClientCustomizer;
import org.springframework.boot.restclient.autoconfigure.RestClientAutoConfiguration;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.registry.HttpServiceProxyRegistry;
import org.springframework.web.service.registry.ImportHttpServices;

/**
 * AutoConfiguration for Spring HTTP Service clients.
 * <p>
 * This will result in the creation of blocking HTTP Service client beans defined by
 * {@link ImportHttpServices @ImportHttpServices} annotations.
 *
 * @author Olga Maciaszek-Sharma
 * @author Rossen Stoyanchev
 * @author Phillip Webb
 * @since 4.0.0
 */
@AutoConfiguration(after = { HttpClientAutoConfiguration.class, RestClientAutoConfiguration.class })
@ConditionalOnClass(RestClientAdapter.class)
@ConditionalOnBean(HttpServiceProxyRegistry.class)
@Conditional(NotReactiveWebApplicationCondition.class)
@EnableConfigurationProperties(HttpClientServiceProperties.class)
public class HttpServiceClientAutoConfiguration implements BeanClassLoaderAware {

	private ClassLoader beanClassLoader;

	HttpServiceClientAutoConfiguration() {
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.beanClassLoader = classLoader;
	}

	@Bean
	RestClientPropertiesHttpServiceGroupConfigurer restClientPropertiesHttpServiceGroupConfigurer(
			ObjectProvider<SslBundles> sslBundles, ObjectProvider<HttpClientProperties> httpClientProperties,
			HttpClientServiceProperties serviceProperties,
			ObjectProvider<ClientHttpRequestFactoryBuilder<?>> clientFactoryBuilder,
			ObjectProvider<ClientHttpRequestFactorySettings> clientHttpRequestFactorySettings) {
		return new RestClientPropertiesHttpServiceGroupConfigurer(this.beanClassLoader, sslBundles,
				httpClientProperties.getIfAvailable(), serviceProperties, clientFactoryBuilder,
				clientHttpRequestFactorySettings);
	}

	@Bean
	RestClientCustomizerHttpServiceGroupConfigurer restClientCustomizerHttpServiceGroupConfigurer(
			ObjectProvider<RestClientCustomizer> customizers) {
		return new RestClientCustomizerHttpServiceGroupConfigurer(customizers);
	}

}
