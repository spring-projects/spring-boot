/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.data.redis.autoconfigure;

import org.springframework.data.redis.connection.jedis.JedisClientConfiguration;
import org.springframework.data.redis.connection.jedis.JedisClientConfiguration.JedisClientConfigurationBuilder;

/**
 * Callback interface that can be implemented by beans wishing to customize the
 * {@link JedisClientConfigurationBuilder} to fine-tune its auto-configuration before it
 * creates the {@link JedisClientConfiguration}.
 *
 * @author Mark Paluch
 * @since 4.0.0
 */
@FunctionalInterface
public interface JedisClientConfigurationBuilderCustomizer {

	/**
	 * Customize the {@link JedisClientConfigurationBuilder}.
	 * @param clientConfigurationBuilder the builder to customize
	 */
	void customize(JedisClientConfigurationBuilder clientConfigurationBuilder);

}
