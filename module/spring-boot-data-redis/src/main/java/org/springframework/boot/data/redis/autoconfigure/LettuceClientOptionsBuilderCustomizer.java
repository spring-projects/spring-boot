/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.data.redis.autoconfigure;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.ClientOptions.Builder;

import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;

/**
 * Callback interface that can be implemented by beans wishing to customize the
 * {@link Builder} to fine-tune its auto-configuration before it creates the
 * {@link ClientOptions} of the {@link LettuceClientConfiguration}. To customize the
 * entire configuration, use {@link LettuceClientConfigurationBuilderCustomizer} instead.
 *
 * @author Soohyun Lim
 * @since 4.0.0
 */
@FunctionalInterface
public interface LettuceClientOptionsBuilderCustomizer {

	/**
	 * Customize the {@link Builder}.
	 * @param clientOptionsBuilder the builder to customize
	 */
	void customize(Builder clientOptionsBuilder);

}
