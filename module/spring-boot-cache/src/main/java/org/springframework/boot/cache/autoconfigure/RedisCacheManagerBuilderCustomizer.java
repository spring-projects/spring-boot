/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.cache.autoconfigure;

import org.springframework.data.redis.cache.RedisCacheManager.RedisCacheManagerBuilder;

/**
 * Callback interface that can be used to customize a {@link RedisCacheManagerBuilder}.
 *
 * @author Dmytro Nosan
 * @since 4.0.0
 */
@FunctionalInterface
public interface RedisCacheManagerBuilderCustomizer {

	/**
	 * Customize the {@link RedisCacheManagerBuilder}.
	 * @param builder the builder to customize
	 */
	void customize(RedisCacheManagerBuilder builder);

}
