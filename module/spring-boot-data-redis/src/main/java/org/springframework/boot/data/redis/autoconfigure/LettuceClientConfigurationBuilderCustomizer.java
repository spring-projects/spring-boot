/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.data.redis.autoconfigure;

import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration.LettuceClientConfigurationBuilder;

/**
 * Callback interface that can be implemented by beans wishing to customize the
 * {@link LettuceClientConfigurationBuilder} to fine-tune its auto-configuration before it
 * creates the {@link LettuceClientConfiguration}. To customize only the
 * {@link LettuceClientConfiguration#getClientOptions() client options} of the
 * configuration, use {@link LettuceClientOptionsBuilderCustomizer} instead.
 *
 * @author Mark Paluch
 * @since 4.0.0
 */
@FunctionalInterface
public interface LettuceClientConfigurationBuilderCustomizer {

	/**
	 * Customize the {@link LettuceClientConfigurationBuilder}.
	 * @param clientConfigurationBuilder the builder to customize
	 */
	void customize(LettuceClientConfigurationBuilder clientConfigurationBuilder);

}
