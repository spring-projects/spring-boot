/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.zipkin.autoconfigure;

import java.net.http.HttpClient.Builder;

/**
 * Callback interface that can be implemented by beans wishing to customize the
 * {@link Builder HttpClient.Builder} used to send spans to Zipkin.
 *
 * @author Moritz Halbritter
 * @since 4.0.0
 */
@FunctionalInterface
public interface ZipkinHttpClientBuilderCustomizer {

	/**
	 * Customize the http client builder.
	 * @param httpClient the http client builder to customize
	 */
	void customize(Builder httpClient);

}
