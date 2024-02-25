/*
 * Copyright 2012-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.autoconfigure.cache;

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

	/**
     * Creates a CacheManager bean using the Cache2k library.
     * 
     * @param cacheProperties The properties for configuring the cache.
     * @param customizers The customizers for configuring the cache manager.
     * @param cache2kBuilderCustomizers The customizers for configuring the cache builder.
     * @return The CacheManager bean.
     */
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

	/**
     * Configures the default settings for a Cache2kBuilder.
     * 
     * @param cache2kBuilderCustomizers The customizers to apply to the Cache2kBuilder.
     * @return The configured Cache2kBuilder.
     */
    private Function<Cache2kBuilder<?, ?>, Cache2kBuilder<?, ?>> configureDefaults(
			ObjectProvider<Cache2kBuilderCustomizer> cache2kBuilderCustomizers) {
		return (builder) -> {
			cache2kBuilderCustomizers.orderedStream().forEach((customizer) -> customizer.customize(builder));
			return builder;
		};
	}

}
