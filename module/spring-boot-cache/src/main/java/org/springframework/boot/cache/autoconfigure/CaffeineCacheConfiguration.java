/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.cache.autoconfigure;

import java.util.List;

import com.github.benmanes.caffeine.cache.AsyncCacheLoader;
import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.CaffeineSpec;
import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.cache.autoconfigure.CacheProperties.Caffeine.CacheMode;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * Caffeine cache configuration.
 *
 * @author Eddú Meléndez
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({ Caffeine.class, CaffeineCacheManager.class })
@ConditionalOnMissingBean(CacheManager.class)
@Conditional({ CacheCondition.class })
class CaffeineCacheConfiguration {

	@Bean
	CaffeineCacheManager cacheManager(CacheProperties cacheProperties, CacheManagerCustomizers customizers,
			ObjectProvider<Caffeine<Object, Object>> caffeine, ObjectProvider<CaffeineSpec> caffeineSpec,
			ObjectProvider<CacheLoader<Object, Object>> cacheLoader,
			ObjectProvider<AsyncCacheLoader<Object, Object>> asyncCacheLoader) {
		CaffeineCacheManager cacheManager = createCacheManager(cacheProperties, caffeine, caffeineSpec, cacheLoader,
				asyncCacheLoader);
		List<String> cacheNames = cacheProperties.getCacheNames();
		if (!CollectionUtils.isEmpty(cacheNames)) {
			cacheManager.setCacheNames(cacheNames);
		}
		return customizers.customize(cacheManager);
	}

	private CaffeineCacheManager createCacheManager(CacheProperties cacheProperties,
			ObjectProvider<Caffeine<Object, Object>> caffeine, ObjectProvider<CaffeineSpec> caffeineSpec,
			ObjectProvider<CacheLoader<Object, Object>> cacheLoader,
			ObjectProvider<AsyncCacheLoader<Object, Object>> asyncCacheLoader) {
		CaffeineCacheManager cacheManager = new CaffeineCacheManager();
		CacheProperties.Caffeine caffeineProperties = cacheProperties.getCaffeine();
		boolean asyncCacheMode = caffeineProperties.getCacheMode() == CacheMode.ASYNC;
		if (asyncCacheMode) {
			cacheManager.setAsyncCacheMode(true);
		}
		setCacheBuilder(caffeineProperties, caffeineSpec.getIfAvailable(), caffeine.getIfAvailable(), cacheManager);
		setCacheLoader(asyncCacheMode, cacheLoader, asyncCacheLoader, cacheManager);
		return cacheManager;
	}

	private void setCacheBuilder(CacheProperties.Caffeine caffeineProperties, @Nullable CaffeineSpec caffeineSpec,
			@Nullable Caffeine<Object, Object> caffeine, CaffeineCacheManager cacheManager) {
		String specification = caffeineProperties.getSpec();
		if (StringUtils.hasText(specification)) {
			cacheManager.setCacheSpecification(specification);
		}
		else if (caffeineSpec != null) {
			cacheManager.setCaffeineSpec(caffeineSpec);
		}
		else if (caffeine != null) {
			cacheManager.setCaffeine(caffeine);
		}
	}

	private void setCacheLoader(boolean asyncCacheMode, ObjectProvider<CacheLoader<Object, Object>> cacheLoader,
			ObjectProvider<AsyncCacheLoader<Object, Object>> asyncCacheLoader, CaffeineCacheManager cacheManager) {
		CacheLoader<Object, Object> loader = cacheLoader.getIfAvailable();
		if (loader != null) {
			cacheManager.setCacheLoader(loader);
		}
		else if (asyncCacheMode) {
			asyncCacheLoader.ifAvailable(cacheManager::setAsyncCacheLoader);
		}
	}

}
