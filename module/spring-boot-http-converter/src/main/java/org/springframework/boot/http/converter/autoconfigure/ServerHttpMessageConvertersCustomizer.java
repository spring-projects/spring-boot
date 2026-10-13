/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.http.converter.autoconfigure;

import org.springframework.http.converter.HttpMessageConverters;
import org.springframework.http.converter.HttpMessageConverters.ServerBuilder;

/**
 * Callback interface that can be used to customize a {@link HttpMessageConverters} for
 * server usage.
 *
 * @author Brian Clozel
 * @since 4.0
 */
@FunctionalInterface
public interface ServerHttpMessageConvertersCustomizer {

	/**
	 * Callback to customize a {@link ServerBuilder} instance.
	 * @param builder the builder to customize
	 */
	void customize(ServerBuilder builder);

}
