/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.restclient.autoconfigure;

import java.util.Collection;
import java.util.List;
import java.util.function.BiFunction;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.boot.http.converter.autoconfigure.ClientHttpMessageConvertersCustomizer;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.boot.restclient.RestTemplateCustomizer;
import org.springframework.boot.restclient.RestTemplateRequestCustomizer;
import org.springframework.http.converter.HttpMessageConverters;
import org.springframework.http.converter.HttpMessageConverters.ClientBuilder;
import org.springframework.util.ObjectUtils;

/**
 * Configure {@link RestTemplateBuilder} with sensible defaults.
 * <p>
 * Can be injected into application code and used to define a custom
 * {@code RestTemplateBuilder} whose configuration is based upon that produced by
 * auto-configuration.
 *
 * @author Stephane Nicoll
 * @since 4.0.0
 * @deprecated since 4.2.0 for removal in 4.4.0 in favor of
 * {@link RestClientBuilderConfigurer}
 */
@Deprecated(since = "4.2.0", forRemoval = true)
@SuppressWarnings("removal")
public final class RestTemplateBuilderConfigurer {

	private @Nullable ClientHttpRequestFactoryBuilder<?> requestFactoryBuilder;

	private @Nullable HttpClientSettings clientSettings;

	private @Nullable List<ClientHttpMessageConvertersCustomizer> httpMessageConvertersCustomizers;

	private @Nullable List<RestTemplateCustomizer> restTemplateCustomizers;

	private @Nullable List<RestTemplateRequestCustomizer<?>> restTemplateRequestCustomizers;

	void setRequestFactoryBuilder(@Nullable ClientHttpRequestFactoryBuilder<?> requestFactoryBuilder) {
		this.requestFactoryBuilder = requestFactoryBuilder;
	}

	void setClientSettings(@Nullable HttpClientSettings clientSettings) {
		this.clientSettings = clientSettings;
	}

	void setHttpMessageConvertersCustomizers(
			@Nullable List<ClientHttpMessageConvertersCustomizer> httpMessageConvertersCustomizers) {
		this.httpMessageConvertersCustomizers = httpMessageConvertersCustomizers;
	}

	void setRestTemplateCustomizers(@Nullable List<RestTemplateCustomizer> restTemplateCustomizers) {
		this.restTemplateCustomizers = restTemplateCustomizers;
	}

	void setRestTemplateRequestCustomizers(
			@Nullable List<RestTemplateRequestCustomizer<?>> restTemplateRequestCustomizers) {
		this.restTemplateRequestCustomizers = restTemplateRequestCustomizers;
	}

	/**
	 * Configure the specified {@link RestTemplateBuilder}. The builder can be further
	 * tuned and default settings can be overridden.
	 * @param builder the {@link RestTemplateBuilder} instance to configure
	 * @return the configured builder
	 */
	public RestTemplateBuilder configure(RestTemplateBuilder builder) {
		if (this.requestFactoryBuilder != null) {
			builder = builder.requestFactoryBuilder(this.requestFactoryBuilder);
		}
		if (this.clientSettings != null) {
			builder = builder.clientSettings(this.clientSettings);
		}
		if (this.httpMessageConvertersCustomizers != null) {
			ClientBuilder clientBuilder = HttpMessageConverters.forClient();
			this.httpMessageConvertersCustomizers.forEach((customizer) -> customizer.customize(clientBuilder));
			builder = builder.messageConverters(clientBuilder.build());
		}
		builder = addCustomizers(builder, this.restTemplateCustomizers, RestTemplateBuilder::customizers);
		builder = addCustomizers(builder, this.restTemplateRequestCustomizers, RestTemplateBuilder::requestCustomizers);
		return builder;
	}

	private <T> RestTemplateBuilder addCustomizers(RestTemplateBuilder builder, @Nullable List<T> customizers,
			BiFunction<RestTemplateBuilder, Collection<T>, RestTemplateBuilder> method) {
		if (!ObjectUtils.isEmpty(customizers)) {
			return method.apply(builder, customizers);
		}
		return builder;
	}

}
