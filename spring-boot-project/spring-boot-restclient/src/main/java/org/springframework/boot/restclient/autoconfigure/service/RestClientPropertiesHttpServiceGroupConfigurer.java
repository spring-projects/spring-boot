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

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.boot.http.client.autoconfigure.ClientHttpRequestFactories;
import org.springframework.boot.http.client.autoconfigure.HttpClientProperties;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientHttpServiceGroupConfigurer;
import org.springframework.web.service.registry.HttpServiceGroup;

/**
 * A {@link RestClientHttpServiceGroupConfigurer} that configures the group and its
 * underlying {@link RestClient} using {@link HttpClientProperties}.
 *
 * @author Olga Maciaszek-Sharma
 * @author Phillip Webb
 */
class RestClientPropertiesHttpServiceGroupConfigurer implements RestClientHttpServiceGroupConfigurer {

	private final ClassLoader classLoader;

	private final ObjectProvider<SslBundles> sslBundles;

	private final HttpClientProperties clientProperties;

	private final HttpClientServiceProperties serviceProperties;

	private final ObjectProvider<ClientHttpRequestFactoryBuilder<?>> requestFactoryBuilder;

	private final ObjectProvider<ClientHttpRequestFactorySettings> requestFactorySettings;

	RestClientPropertiesHttpServiceGroupConfigurer(ClassLoader classLoader, ObjectProvider<SslBundles> sslBundles,
			HttpClientProperties clientProperties, HttpClientServiceProperties serviceProperties,
			ObjectProvider<ClientHttpRequestFactoryBuilder<?>> requestFactoryBuilder,
			ObjectProvider<ClientHttpRequestFactorySettings> requestFactorySettings) {
		this.classLoader = classLoader;
		this.sslBundles = sslBundles;
		this.clientProperties = clientProperties;
		this.serviceProperties = serviceProperties;
		this.requestFactoryBuilder = requestFactoryBuilder;
		this.requestFactorySettings = requestFactorySettings;
	}

	@Override
	public int getOrder() {
		return Ordered.HIGHEST_PRECEDENCE;
	}

	@Override
	public void configureGroups(Groups<RestClient.Builder> groups) {
		groups.forEachClient(this::configureClient);
	}

	private void configureClient(HttpServiceGroup group, RestClient.Builder builder) {
		HttpClientServiceProperties.Group groupProperties = this.serviceProperties.getGroup().get(group.name());
		builder.requestFactory(getRequestFactory(groupProperties));
		PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
		map.from(this.serviceProperties::getBaseUrl).whenHasText().to(builder::baseUrl);
		map.from(this.serviceProperties::getDefaultHeader).as(this::putAllHeaders).to(builder::defaultHeaders);
		if (groupProperties != null) {
			map.from(groupProperties::getBaseUrl).whenHasText().to(builder::baseUrl);
			map.from(groupProperties::getDefaultHeader).as(this::putAllHeaders).to(builder::defaultHeaders);
		}
	}

	private Consumer<HttpHeaders> putAllHeaders(Map<String, List<String>> defaultHeaders) {
		return (httpHeaders) -> httpHeaders.putAll(defaultHeaders);
	}

	private ClientHttpRequestFactory getRequestFactory(HttpClientServiceProperties.Group groupProperties) {
		ClientHttpRequestFactories factories = new ClientHttpRequestFactories(this.sslBundles, groupProperties,
				this.serviceProperties, this.clientProperties);
		ClientHttpRequestFactoryBuilder<?> builder = this.requestFactoryBuilder
			.getIfAvailable(() -> factories.builder(this.classLoader));
		ClientHttpRequestFactorySettings settings = this.requestFactorySettings.getIfAvailable(factories::settings);
		return builder.build(settings);
	}

}
