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

package org.springframework.boot.restclient.autoconfigure;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.ssl.SslAutoConfiguration;
import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.boot.http.client.autoconfigure.imperative.ImperativeHttpClientAutoConfiguration;
import org.springframework.boot.http.converter.autoconfigure.ClientHttpMessageConvertersCustomizer;
import org.springframework.boot.restclient.RestClientCustomizer;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.converter.HttpMessageConverters;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClient.Builder;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link RestClient}.
 * <p>
 * This will produce a {@link Builder RestClient.Builder} bean with the {@code prototype}
 * scope, meaning each injection point will receive a newly cloned instance of the
 * builder.
 *
 * @author Arjen Poutsma
 * @author Moritz Halbritter
 * @author Phillip Webb
 * @since 4.0.0
 */
@AutoConfiguration(after = { ImperativeHttpClientAutoConfiguration.class, TaskExecutionAutoConfiguration.class,
		SslAutoConfiguration.class })
@ConditionalOnClass(RestClient.class)
public final class RestClientAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean(RestClientSsl.class)
	@ConditionalOnBean(SslBundles.class)
	AutoConfiguredRestClientSsl restClientSsl(ResourceLoader resourceLoader,
			ObjectProvider<ClientHttpRequestFactoryBuilder<?>> clientHttpRequestFactoryBuilder,
			ObjectProvider<HttpClientSettings> httpClientSettings, SslBundles sslBundles) {
		ClassLoader classLoader = resourceLoader.getClassLoader();
		return new AutoConfiguredRestClientSsl(
				clientHttpRequestFactoryBuilder
					.getIfAvailable(() -> ClientHttpRequestFactoryBuilder.detect(classLoader)),
				httpClientSettings.getIfAvailable(HttpClientSettings::defaults), sslBundles);
	}

	@Bean
	@ConditionalOnMissingBean
	RestClientBuilderConfigurer restClientBuilderConfigurer(ResourceLoader resourceLoader,
			ObjectProvider<ClientHttpRequestFactoryBuilder<?>> clientHttpRequestFactoryBuilder,
			ObjectProvider<HttpClientSettings> httpClientSettings,
			ObjectProvider<RestClientCustomizer> customizerProvider) {
		return new RestClientBuilderConfigurer(
				clientHttpRequestFactoryBuilder
					.getIfAvailable(() -> ClientHttpRequestFactoryBuilder.detect(resourceLoader.getClassLoader())),
				httpClientSettings.getIfAvailable(HttpClientSettings::defaults),
				customizerProvider.orderedStream().toList());
	}

	@Bean
	@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	@ConditionalOnMissingBean
	RestClient.Builder restClientBuilder(RestClientBuilderConfigurer restClientBuilderConfigurer) {
		return restClientBuilderConfigurer.configure(RestClient.builder());
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(HttpMessageConverters.class)
	static class HttpMessageConvertersConfiguration {

		@Bean
		@ConditionalOnBean(ClientHttpMessageConvertersCustomizer.class)
		@Order(Ordered.LOWEST_PRECEDENCE)
		HttpMessageConvertersRestClientCustomizer httpMessageConvertersRestClientCustomizer(
				ObjectProvider<ClientHttpMessageConvertersCustomizer> customizerProvider) {
			return new HttpMessageConvertersRestClientCustomizer(customizerProvider.orderedStream().toList());
		}

	}

}
