/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.r2dbc.autoconfigure;

import io.r2dbc.proxy.ProxyConnectionFactory.Builder;

/**
 * Callback interface that can be used to customize a {@link Builder}.
 *
 * @author Tadaya Tsuyukubo
 * @since 4.0.0
 */
@FunctionalInterface
public interface ProxyConnectionFactoryCustomizer {

	/**
	 * Callback to customize a {@link Builder} instance.
	 * @param builder the builder to customize
	 */
	void customize(Builder builder);

}
