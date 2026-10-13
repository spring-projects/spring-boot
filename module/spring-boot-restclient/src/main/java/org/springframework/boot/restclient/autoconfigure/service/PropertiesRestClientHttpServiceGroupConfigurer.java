/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.restclient.autoconfigure.service;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.boot.http.client.autoconfigure.ApiversionProperties;
import org.springframework.boot.http.client.autoconfigure.HttpClientProperties;
import org.springframework.boot.http.client.autoconfigure.HttpClientSettingsProperties;
import org.springframework.boot.http.client.autoconfigure.HttpClientSettingsPropertyMapper;
import org.springframework.boot.http.client.autoconfigure.PropertiesApiVersionInserter;
import org.springframework.boot.http.client.autoconfigure.service.HttpServiceClientProperties;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientHttpServiceGroupConfigurer;
import org.springframework.web.service.registry.HttpServiceGroup;

/**
 * A {@link RestClientHttpServiceGroupConfigurer} that configures the group and its
 * underlying {@link RestClient} using {@link HttpClientSettingsProperties}.
 *
 * @author Olga Maciaszek-Sharma
 * @author Phillip Webb
 */
class PropertiesRestClientHttpServiceGroupConfigurer implements RestClientHttpServiceGroupConfigurer {

	/**
	 * The default order for the PropertiesRestClientHttpServiceGroupConfigurer.
	 */
	private static final int DEFAULT_ORDER = Ordered.HIGHEST_PRECEDENCE + 10;

	private final HttpServiceClientProperties properties;

	private final HttpClientSettingsPropertyMapper clientSettingsPropertyMapper;

	private final ClientHttpRequestFactoryBuilder<?> requestFactoryBuilder;

	PropertiesRestClientHttpServiceGroupConfigurer(@Nullable ClassLoader classLoader,
			HttpServiceClientProperties properties, @Nullable SslBundles sslBundles,
			ObjectProvider<ClientHttpRequestFactoryBuilder<?>> requestFactoryBuilder,
			@Nullable HttpClientSettings httpClientSettings) {
		this.properties = properties;
		this.clientSettingsPropertyMapper = new HttpClientSettingsPropertyMapper(sslBundles, httpClientSettings);
		this.requestFactoryBuilder = requestFactoryBuilder
			.getIfAvailable(() -> ClientHttpRequestFactoryBuilder.detect(classLoader));
	}

	@Override
	public int getOrder() {
		return DEFAULT_ORDER;
	}

	@Override
	public void configureGroups(Groups<RestClient.Builder> groups) {
		groups.forEachClient(this::configureClient);
	}

	private void configureClient(HttpServiceGroup group, RestClient.Builder builder) {
		HttpClientProperties clientProperties = this.properties.get(group.name());
		HttpClientSettings clientSettings = this.clientSettingsPropertyMapper.map(clientProperties);
		builder.requestFactory(this.requestFactoryBuilder.build(clientSettings));
		if (clientProperties != null) {
			PropertyMapper map = PropertyMapper.get();
			map.from(clientProperties::getBaseUrl).whenHasText().to(builder::baseUrl);
			map.from(clientProperties::getDefaultHeader).as(this::putAllHeaders).to(builder::defaultHeaders);
			map.from(clientProperties::getApiversion)
				.as(ApiversionProperties::getDefaultVersion)
				.to(builder::defaultApiVersion);
			map.from(clientProperties::getApiversion)
				.as(ApiversionProperties::getInsert)
				.as(PropertiesApiVersionInserter::get)
				.to(builder::apiVersionInserter);
		}
	}

	private Consumer<HttpHeaders> putAllHeaders(Map<String, List<String>> defaultHeaders) {
		return (httpHeaders) -> httpHeaders.putAll(defaultHeaders);
	}

}
