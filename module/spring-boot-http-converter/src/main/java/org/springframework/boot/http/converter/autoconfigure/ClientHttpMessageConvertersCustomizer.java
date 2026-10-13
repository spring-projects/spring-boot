/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.http.converter.autoconfigure;

import org.springframework.http.converter.HttpMessageConverters;
import org.springframework.http.converter.HttpMessageConverters.ClientBuilder;

/**
 * Callback interface that can be used to customize a {@link HttpMessageConverters} for
 * client usage.
 *
 * @author Brian Clozel
 * @since 4.0
 */
@FunctionalInterface
public interface ClientHttpMessageConvertersCustomizer {

	/**
	 * Callback to customize a {@link ClientBuilder HttpMessageConverters.ClientBuilder}
	 * instance.
	 * @param builder the builder to customize
	 */
	void customize(ClientBuilder builder);

}
