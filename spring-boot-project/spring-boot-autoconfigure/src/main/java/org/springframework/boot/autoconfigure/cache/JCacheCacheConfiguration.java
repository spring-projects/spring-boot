/*
 * Copyright 2012-2019 the original author or authors.
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

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.spi.CachingProvider;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.autoconfigure.condition.ConditionMessage;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.cache.jcache.JCacheCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * Cache configuration for JSR-107 compliant providers.
 *
 * @author Stephane Nicoll
 * @author Madhura Bhave
 * @since 1.3.0
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({ Caching.class, JCacheCacheManager.class })
@ConditionalOnMissingBean(org.springframework.cache.CacheManager.class)
@Conditional({ CacheCondition.class,
		JCacheCacheConfiguration.JCacheAvailableCondition.class })
@Import(HazelcastJCacheCustomizationConfiguration.class)
class JCacheCacheConfiguration implements BeanClassLoaderAware {

	private ClassLoader beanClassLoader;

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.beanClassLoader = classLoader;
	}

	@Bean
	public JCacheCacheManager cacheManager(CacheManagerCustomizers customizers,
			CacheManager jCacheCacheManager) {
		JCacheCacheManager cacheManager = new JCacheCacheManager(jCacheCacheManager);
		return customizers.customize(cacheManager);
	}

	@Bean
	@ConditionalOnMissingBean
	public CacheManager jCacheCacheManager(CacheProperties cacheProperties,
			ObjectProvider<javax.cache.configuration.Configuration<?, ?>> defaultCacheConfiguration,
			ObjectProvider<JCacheManagerCustomizer> cacheManagerCustomizers,
			ObjectProvider<JCachePropertiesCustomizer> cachePropertiesCustomizers)
			throws IOException {
		CacheManager jCacheCacheManager = createCacheManager(cacheProperties,
				cachePropertiesCustomizers);
		List<String> cacheNames = cacheProperties.getCacheNames();
		if (!CollectionUtils.isEmpty(cacheNames)) {
			for (String cacheName : cacheNames) {
				jCacheCacheManager.createCache(cacheName, defaultCacheConfiguration
						.getIfAvailable(MutableConfiguration::new));
			}
		}
		cacheManagerCustomizers.orderedStream()
				.forEach((customizer) -> customizer.customize(jCacheCacheManager));
		return jCacheCacheManager;
	}

	private CacheManager createCacheManager(CacheProperties cacheProperties,
			ObjectProvider<JCachePropertiesCustomizer> cachePropertiesCustomizers)
			throws IOException {
		CachingProvider cachingProvider = getCachingProvider(
				cacheProperties.getJcache().getProvider());
		Properties properties = createCacheManagerProperties(cachePropertiesCustomizers,
				cacheProperties);
		Resource configLocation = cacheProperties
				.resolveConfigLocation(cacheProperties.getJcache().getConfig());
		if (configLocation != null) {
			return cachingProvider.getCacheManager(configLocation.getURI(),
					this.beanClassLoader, properties);
		}
		return cachingProvider.getCacheManager(null, this.beanClassLoader, properties);
	}

	private CachingProvider getCachingProvider(String cachingProviderFqn) {
		if (StringUtils.hasText(cachingProviderFqn)) {
			return Caching.getCachingProvider(cachingProviderFqn);
		}
		return Caching.getCachingProvider();
	}

	private Properties createCacheManagerProperties(
			ObjectProvider<JCachePropertiesCustomizer> cachePropertiesCustomizers,
			CacheProperties cacheProperties) {
		Properties properties = new Properties();
		cachePropertiesCustomizers.orderedStream().forEach(
				(customizer) -> customizer.customize(cacheProperties, properties));
		return properties;
	}

	/**
	 * Determine if JCache is available. This either kicks in if a provider is available
	 * as defined per {@link JCacheProviderAvailableCondition} or if a
	 * {@link CacheManager} has already been defined.
	 */
	@Order(Ordered.LOWEST_PRECEDENCE)
	static class JCacheAvailableCondition extends AnyNestedCondition {

		JCacheAvailableCondition() {
			super(ConfigurationPhase.REGISTER_BEAN);
		}

		@Conditional(JCacheProviderAvailableCondition.class)
		static class JCacheProvider {

		}

		@ConditionalOnSingleCandidate(CacheManager.class)
		static class CustomJCacheCacheManager {

		}

	}

	/**
	 * Determine if a JCache provider is available. This either kicks in if a default
	 * {@link CachingProvider} has been found or if the property referring to the provider
	 * to use has been set.
	 */
	@Order(Ordered.LOWEST_PRECEDENCE)
	static class JCacheProviderAvailableCondition extends SpringBootCondition {

		@Override
		public ConditionOutcome getMatchOutcome(ConditionContext context,
				AnnotatedTypeMetadata metadata) {
			ConditionMessage.Builder message = ConditionMessage.forCondition("JCache");
			String providerProperty = "spring.cache.jcache.provider";
			if (context.getEnvironment().containsProperty(providerProperty)) {
				return ConditionOutcome
						.match(message.because("JCache provider specified"));
			}
			Iterator<CachingProvider> providers = Caching.getCachingProviders()
					.iterator();
			if (!providers.hasNext()) {
				return ConditionOutcome
						.noMatch(message.didNotFind("JSR-107 provider").atAll());
			}
			providers.next();
			if (providers.hasNext()) {
				return ConditionOutcome
						.noMatch(message.foundExactly("multiple JSR-107 providers"));

			}
			return ConditionOutcome
					.match(message.foundExactly("single JSR-107 provider"));
		}

	}

}
