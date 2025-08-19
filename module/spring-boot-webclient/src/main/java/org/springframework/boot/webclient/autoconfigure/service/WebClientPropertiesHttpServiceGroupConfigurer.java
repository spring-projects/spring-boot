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

import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.http.client.autoconfigure.reactive.ClientHttpConnectors;
import org.springframework.boot.http.client.autoconfigure.reactive.HttpReactiveClientProperties;
import org.springframework.boot.http.client.reactive.ClientHttpConnectorBuilder;
import org.springframework.boot.http.client.reactive.ClientHttpConnectorSettings;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.boot.webclient.autoconfigure.PropertiesWebClientCustomizer;
import org.springframework.core.Ordered;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.web.client.ApiVersionFormatter;
import org.springframework.web.client.ApiVersionInserter;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientHttpServiceGroupConfigurer;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientHttpServiceGroupConfigurer;
import org.springframework.web.service.registry.HttpServiceGroup;

/**
 * A {@link RestClientHttpServiceGroupConfigurer} that configures the group and its
 * underlying {@link RestClient} using {@link HttpReactiveClientProperties}.
 *
 * @author Olga Maciaszek-Sharma
 * @author Phillip Webb
 */
class WebClientPropertiesHttpServiceGroupConfigurer implements WebClientHttpServiceGroupConfigurer {

	private final ClassLoader classLoader;

	private final ObjectProvider<SslBundles> sslBundles;

	private final ReactiveHttpClientServiceProperties serviceProperties;

	private final ObjectProvider<ClientHttpConnectorBuilder<?>> clientConnectorBuilder;

	private final ObjectProvider<ClientHttpConnectorSettings> clientConnectorSettings;

	private final @Nullable ApiVersionInserter apiVersionInserter;

	private final @Nullable ApiVersionFormatter apiVersionFormatter;

	WebClientPropertiesHttpServiceGroupConfigurer(ClassLoader classLoader, ObjectProvider<SslBundles> sslBundles,
			ReactiveHttpClientServiceProperties serviceProperties,
			ObjectProvider<ClientHttpConnectorBuilder<?>> clientConnectorBuilder,
			ObjectProvider<ClientHttpConnectorSettings> clientConnectorSettings,
			ObjectProvider<ApiVersionInserter> apiVersionInserter,
			ObjectProvider<ApiVersionFormatter> apiVersionFormatter) {
		this.classLoader = classLoader;
		this.sslBundles = sslBundles;
		this.serviceProperties = serviceProperties;
		this.clientConnectorBuilder = clientConnectorBuilder;
		this.clientConnectorSettings = clientConnectorSettings;
		this.apiVersionInserter = apiVersionInserter.getIfAvailable();
		this.apiVersionFormatter = apiVersionFormatter.getIfAvailable();
	}

	@Override
	public int getOrder() {
		return Ordered.HIGHEST_PRECEDENCE;
	}

	@Override
	public void configureGroups(Groups<WebClient.Builder> groups) {
		groups.forEachClient(this::configureClient);
	}

	private void configureClient(HttpServiceGroup group, WebClient.Builder builder) {
		ReactiveHttpClientServiceProperties.Group groupProperties = this.serviceProperties.getGroup().get(group.name());
		builder.clientConnector(getClientConnector(groupProperties));
		getPropertiesWebClientCustomizer(groupProperties).customize(builder);
	}

	private PropertiesWebClientCustomizer getPropertiesWebClientCustomizer(
			ReactiveHttpClientServiceProperties.@Nullable Group groupProperties) {
		return new PropertiesWebClientCustomizer(this.apiVersionInserter, this.apiVersionFormatter, groupProperties,
				this.serviceProperties);
	}

	private ClientHttpConnector getClientConnector(
			ReactiveHttpClientServiceProperties.@Nullable Group groupProperties) {
		ClientHttpConnectors connectors = new ClientHttpConnectors(this.clientConnectorBuilder.getIfAvailable(),
				this.clientConnectorSettings.getIfAvailable(), this.sslBundles, groupProperties,
				this.serviceProperties);
		ClientHttpConnectorBuilder<?> builder = connectors.builder(this.classLoader);
		ClientHttpConnectorSettings settings = connectors.settings();
		return builder.build(settings);
	}

}
