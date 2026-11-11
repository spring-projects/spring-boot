/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.webclient.autoconfigure.service;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.boot.http.client.autoconfigure.reactive.ReactiveHttpClientAutoConfiguration;
import org.springframework.boot.http.client.autoconfigure.service.HttpServiceClientProperties;
import org.springframework.boot.http.client.autoconfigure.service.HttpServiceClientPropertiesAutoConfiguration;
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
 * {@link EnableAutoConfiguration Auto-configuration} for Spring reactive HTTP Service
 * Clients backed by {@link WebClient}.
 *
 * @author Olga Maciaszek-Sharma
 * @author Rossen Stoyanchev
 * @author Phillip Webb
 * @since 4.0.0
 */
@AutoConfiguration(after = { HttpServiceClientPropertiesAutoConfiguration.class,
		ReactiveHttpClientAutoConfiguration.class, WebClientAutoConfiguration.class })
@ConditionalOnClass(WebClientAdapter.class)
@ConditionalOnBean(HttpServiceProxyRegistry.class)
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
