/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.webclient.autoconfigure.service;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.boot.http.client.autoconfigure.ApiversionProperties;
import org.springframework.boot.http.client.autoconfigure.HttpClientProperties;
import org.springframework.boot.http.client.autoconfigure.HttpClientSettingsPropertyMapper;
import org.springframework.boot.http.client.autoconfigure.PropertiesApiVersionInserter;
import org.springframework.boot.http.client.autoconfigure.service.HttpServiceClientProperties;
import org.springframework.boot.http.client.reactive.ClientHttpConnectorBuilder;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientHttpServiceGroupConfigurer;
import org.springframework.web.service.registry.HttpServiceGroup;

/**
 * A {@link WebClientHttpServiceGroupConfigurer} that configures the group and its
 * underlying {@link WebClient} using {@link HttpServiceClientProperties}.
 *
 * @author Olga Maciaszek-Sharma
 * @author Phillip Webb
 */
class PropertiesWebClientHttpServiceGroupConfigurer implements WebClientHttpServiceGroupConfigurer {

	/**
	 * The default order for the PropertiesWebClientHttpServiceGroupConfigurer.
	 */
	private static final int DEFAULT_ORDER = Ordered.HIGHEST_PRECEDENCE + 10;

	private final HttpServiceClientProperties properties;

	private final HttpClientSettingsPropertyMapper clientSettingsPropertyMapper;

	private final ClientHttpConnectorBuilder<?> clientConnectorBuilder;

	PropertiesWebClientHttpServiceGroupConfigurer(@Nullable ClassLoader classLoader,
			HttpServiceClientProperties properties, @Nullable SslBundles sslBundles,
			ObjectProvider<ClientHttpConnectorBuilder<?>> clientConnectorBuilder,
			@Nullable HttpClientSettings httpClientSettings) {
		this.properties = properties;
		this.clientSettingsPropertyMapper = new HttpClientSettingsPropertyMapper(sslBundles, httpClientSettings);
		this.clientConnectorBuilder = clientConnectorBuilder
			.getIfAvailable(() -> ClientHttpConnectorBuilder.detect(classLoader));
	}

	@Override
	public int getOrder() {
		return DEFAULT_ORDER;
	}

	@Override
	public void configureGroups(Groups<WebClient.Builder> groups) {
		groups.forEachClient(this::configureClient);
	}

	private void configureClient(HttpServiceGroup group, WebClient.Builder builder) {
		HttpClientProperties clientProperties = this.properties.get(group.name());
		HttpClientSettings clientSettings = this.clientSettingsPropertyMapper.map(clientProperties);
		builder.clientConnector(this.clientConnectorBuilder.build(clientSettings));
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
