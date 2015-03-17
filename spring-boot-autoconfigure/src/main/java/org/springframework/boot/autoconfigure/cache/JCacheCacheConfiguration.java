/*
 * Copyright 2012-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.autoconfigure.cache;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.spi.CachingProvider;

import org.apache.commons.collections.CollectionUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.cache.jcache.JCacheCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.StringUtils;

/**
 * Cache configuration for JSR-107 compliant providers.
 *
 * @author Stephane Nicoll
 * @since 1.3.0
 */
@Configuration
@ConditionalOnMissingBean({org.springframework.cache.CacheManager.class})
@ConditionalOnClass(Caching.class)
@Conditional(JCacheCacheConfiguration.JCacheAvailableCondition.class)
@ConditionalOnProperty(prefix = "spring.cache", value = "mode", havingValue = "jcache", matchIfMissing = true)
class JCacheCacheConfiguration {

	@Autowired
	private CacheProperties cacheProperties;

	@Autowired(required = false)
	private javax.cache.configuration.Configuration<?, ?> defaultCacheConfiguration;

	@Autowired(required = false)
	private List<JCacheManagerCustomizer> cacheManagerCustomizers;

	@Bean
	public JCacheCacheManager cacheManager() {
		CacheManager cacheManager = createCacheManager(this.cacheProperties.getJcache().getProvider());
		List<String> cacheNames = this.cacheProperties.getCacheNames();
		if (!CollectionUtils.isEmpty(cacheNames)) {
			for (String cacheName : cacheNames) {
				cacheManager.createCache(cacheName, getDefaultCacheConfiguration());
			}
		}
		customize(cacheManager);
		return new JCacheCacheManager(cacheManager);
	}

	private CacheManager createCacheManager(String cachingProvider) {
		if (StringUtils.hasText(cachingProvider)) {
			return Caching.getCachingProvider(cachingProvider).getCacheManager();
		}
		else {
			return Caching.getCachingProvider().getCacheManager();
		}
	}

	private javax.cache.configuration.Configuration<?, ?> getDefaultCacheConfiguration() {
		if (this.defaultCacheConfiguration != null) {
			return this.defaultCacheConfiguration;
		}
		return new MutableConfiguration<Object, Object>();
	}

	private void customize(CacheManager cacheManager) {
		if (this.cacheManagerCustomizers != null) {
			AnnotationAwareOrderComparator.sort(this.cacheManagerCustomizers);
			for (JCacheManagerCustomizer customizer : this.cacheManagerCustomizers) {
				customizer.customize(cacheManager);
			}
		}
	}

	/**
	 * Determines if JCache is available. This either kick in if a default {@link CachingProvider}
	 * has been found or if the property referring to the provider to use has been set.
	 */
	static class JCacheAvailableCondition extends AnyNestedCondition {

		public JCacheAvailableCondition() {
			super(ConfigurationPhase.PARSE_CONFIGURATION);
		}

		@Conditional(DefaultCachingProviderAvailableCondition.class)
		static class DefaultCachingProviderAvailable {
		}

		@ConditionalOnProperty(prefix = "spring.cache.jcache", name = "provider")
		static class CachingProviderProperty {
		}

	}

	static class DefaultCachingProviderAvailableCondition extends SpringBootCondition {

		@Override
		public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
			int cachingProvidersCount = getCachingProvidersCount();
			if (cachingProvidersCount == 1) {
				return ConditionOutcome.match("Default JSR-107 compliant provider found.");
			}
			else {
				return ConditionOutcome.noMatch("No default JSR-107 compliant provider(s) " +
						"found (" + cachingProvidersCount + " provider(s) detected).");
			}
		}

		private int getCachingProvidersCount() {
			Iterable<CachingProvider> cachingProviders = Caching.getCachingProviders();
			if (cachingProviders instanceof Collection<?>) {
				return ((Collection<?>) cachingProviders).size();
			}
			else {
				int count = 0;
				Iterator<CachingProvider> it = cachingProviders.iterator();
				while (it.hasNext()) {
					count++;
				}
				return count;
			}
		}
	}

}
