/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.cache.autoconfigure;

import java.util.Collection;
import java.util.function.Function;

import org.cache2k.Cache2kBuilder;
import org.cache2k.extra.spring.SpringCache2kCacheManager;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.CollectionUtils;

/**
 * Cache2k cache configuration.
 *
 * @author Jens Wilke
 * @author Stephane Nicoll
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({ Cache2kBuilder.class, SpringCache2kCacheManager.class })
@ConditionalOnMissingBean(CacheManager.class)
@Conditional(CacheCondition.class)
class Cache2kCacheConfiguration {

	@Bean
	SpringCache2kCacheManager cacheManager(CacheProperties cacheProperties, CacheManagerCustomizers customizers,
			ObjectProvider<Cache2kBuilderCustomizer> cache2kBuilderCustomizers) {
		SpringCache2kCacheManager cacheManager = new SpringCache2kCacheManager();
		cacheManager.defaultSetup(configureDefaults(cache2kBuilderCustomizers));
		Collection<String> cacheNames = cacheProperties.getCacheNames();
		if (!CollectionUtils.isEmpty(cacheNames)) {
			cacheManager.setDefaultCacheNames(cacheNames);
		}
		return customizers.customize(cacheManager);
	}

	private Function<Cache2kBuilder<?, ?>, Cache2kBuilder<?, ?>> configureDefaults(
			ObjectProvider<Cache2kBuilderCustomizer> cache2kBuilderCustomizers) {
		return (builder) -> {
			cache2kBuilderCustomizers.orderedStream().forEach((customizer) -> customizer.customize(builder));
			return builder;
		};
	}

}
