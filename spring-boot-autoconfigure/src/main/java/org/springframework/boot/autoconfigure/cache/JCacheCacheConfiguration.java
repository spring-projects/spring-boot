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

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.spi.CachingProvider;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.boot.bind.RelaxedPropertyResolver;
import org.springframework.cache.jcache.JCacheCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * Cache configuration for JSR-107 compliant providers.
 *
 * @author Stephane Nicoll
 * @since 1.3.0
 */
@Configuration
@ConditionalOnClass({ Caching.class, JCacheCacheManager.class })
@ConditionalOnMissingBean(org.springframework.cache.CacheManager.class)
@Conditional({ CacheCondition.class,
		JCacheCacheConfiguration.JCacheAvailableCondition.class })
class JCacheCacheConfiguration {

	@Autowired
	private CacheProperties cacheProperties;

	@Autowired(required = false)
	private javax.cache.configuration.Configuration<?, ?> defaultCacheConfiguration;

	@Autowired(required = false)
	private List<JCacheManagerCustomizer> cacheManagerCustomizers;

	@Bean
	public JCacheCacheManager cacheManager(CacheManager jCacheCacheManager) {
		return new JCacheCacheManager(jCacheCacheManager);
	}

	@Bean
	@ConditionalOnMissingBean
	public CacheManager jCacheCacheManager() throws IOException {
		CachingProvider cachingProvider = getCachingProvider(this.cacheProperties
				.getJcache().getProvider());
		Resource configLocation = this.cacheProperties.resolveConfigLocation();
		if (configLocation != null) {
			return cachingProvider.getCacheManager(configLocation.getURI(),
					cachingProvider.getDefaultClassLoader(),
					createCacheManagerProperties(configLocation));
		}
		CacheManager jCacheCacheManager = cachingProvider.getCacheManager();
		List<String> cacheNames = this.cacheProperties.getCacheNames();
		if (!CollectionUtils.isEmpty(cacheNames)) {
			for (String cacheName : cacheNames) {
				jCacheCacheManager.createCache(cacheName, getDefaultCacheConfiguration());
			}
		}
		customize(jCacheCacheManager);
		return jCacheCacheManager;
	}

	private CachingProvider getCachingProvider(String cachingProviderFqn) {
		if (StringUtils.hasText(cachingProviderFqn)) {
			return Caching.getCachingProvider(cachingProviderFqn);
		}
		return Caching.getCachingProvider();
	}

	private Properties createCacheManagerProperties(Resource configLocation)
			throws IOException {
		Properties properties = new Properties();
		// Hazelcast does not use the URI as a mean to specify a custom config.
		properties.setProperty("hazelcast.config.location", configLocation.getURI()
				.toString());
		return properties;
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
	 * Determine if JCache is available. This either kick in if a provider is available
	 * as defined per {@link JCacheProviderAvailableCondition} or if a {@link CacheManager}
	 * has already been defined.
	 */
	@Order(Ordered.LOWEST_PRECEDENCE)
	static class JCacheAvailableCondition extends AnyNestedCondition {

		public JCacheAvailableCondition() {
			super(ConfigurationPhase.REGISTER_BEAN);
		}

		@Conditional(JCacheProviderAvailableCondition.class)
		static class JCacheProvider {}

		@ConditionalOnSingleCandidate(CacheManager.class)
		static class CustomJCacheCacheManager {}

	}

	/**
	 * Determine if a JCache provider is available. This either kick in if a default
	 * {@link CachingProvider} has been found or if the property referring to the provider
	 * to use has been set.
	 */
	@Order(Ordered.LOWEST_PRECEDENCE)
	static class JCacheProviderAvailableCondition extends SpringBootCondition {

		@Override
		public ConditionOutcome getMatchOutcome(ConditionContext context,
				AnnotatedTypeMetadata metadata) {
			RelaxedPropertyResolver resolver = new RelaxedPropertyResolver(
					context.getEnvironment(), "spring.cache.jcache.");
			if (resolver.containsProperty("provider")) {
				return ConditionOutcome.match("JCache provider specified");
			}
			Iterator<CachingProvider> providers = Caching.getCachingProviders()
					.iterator();
			if (!providers.hasNext()) {
				return ConditionOutcome.noMatch("No JSR-107 compliant providers");
			}
			providers.next();
			if (providers.hasNext()) {
				return ConditionOutcome.noMatch("Multiple default JSR-107 compliant "
						+ "providers found");

			}
			return ConditionOutcome.match("Default JSR-107 compliant provider found.");
		}

	}

}
