/*
 * Copyright 2012-2023 the original author or authors.
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
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({ Caching.class, JCacheCacheManager.class })
@ConditionalOnMissingBean(org.springframework.cache.CacheManager.class)
@Conditional({ CacheCondition.class, JCacheCacheConfiguration.JCacheAvailableCondition.class })
@Import(HazelcastJCacheCustomizationConfiguration.class)
class JCacheCacheConfiguration implements BeanClassLoaderAware {

	private ClassLoader beanClassLoader;

	/**
     * Set the class loader to be used for loading beans.
     * 
     * @param classLoader the class loader to be used
     */
    @Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.beanClassLoader = classLoader;
	}

	/**
     * Creates a JCacheCacheManager bean.
     * 
     * @param customizers the CacheManagerCustomizers to apply
     * @param jCacheCacheManager the JCache CacheManager to use
     * @return the JCacheCacheManager bean
     */
    @Bean
	JCacheCacheManager cacheManager(CacheManagerCustomizers customizers, CacheManager jCacheCacheManager) {
		JCacheCacheManager cacheManager = new JCacheCacheManager(jCacheCacheManager);
		return customizers.customize(cacheManager);
	}

	/**
     * Create a JCache CacheManager bean if no other bean of the same type is present.
     * 
     * @param cacheProperties the cache properties
     * @param defaultCacheConfiguration the default cache configuration
     * @param cacheManagerCustomizers the cache manager customizers
     * @param cachePropertiesCustomizers the cache properties customizers
     * @return the JCache CacheManager bean
     * @throws IOException if an I/O error occurs
     */
    @Bean
	@ConditionalOnMissingBean
	CacheManager jCacheCacheManager(CacheProperties cacheProperties,
			ObjectProvider<javax.cache.configuration.Configuration<?, ?>> defaultCacheConfiguration,
			ObjectProvider<JCacheManagerCustomizer> cacheManagerCustomizers,
			ObjectProvider<JCachePropertiesCustomizer> cachePropertiesCustomizers) throws IOException {
		CacheManager jCacheCacheManager = createCacheManager(cacheProperties, cachePropertiesCustomizers);
		List<String> cacheNames = cacheProperties.getCacheNames();
		if (!CollectionUtils.isEmpty(cacheNames)) {
			for (String cacheName : cacheNames) {
				jCacheCacheManager.createCache(cacheName,
						defaultCacheConfiguration.getIfAvailable(MutableConfiguration::new));
			}
		}
		cacheManagerCustomizers.orderedStream().forEach((customizer) -> customizer.customize(jCacheCacheManager));
		return jCacheCacheManager;
	}

	/**
     * Creates a cache manager based on the provided cache properties and cache properties customizers.
     * 
     * @param cacheProperties The cache properties to be used for creating the cache manager.
     * @param cachePropertiesCustomizers The customizers for cache properties.
     * @return The created cache manager.
     * @throws IOException If an I/O error occurs while resolving the cache configuration location.
     */
    private CacheManager createCacheManager(CacheProperties cacheProperties,
			ObjectProvider<JCachePropertiesCustomizer> cachePropertiesCustomizers) throws IOException {
		CachingProvider cachingProvider = getCachingProvider(cacheProperties.getJcache().getProvider());
		Properties properties = createCacheManagerProperties(cachePropertiesCustomizers, cacheProperties);
		Resource configLocation = cacheProperties.resolveConfigLocation(cacheProperties.getJcache().getConfig());
		if (configLocation != null) {
			return cachingProvider.getCacheManager(configLocation.getURI(), this.beanClassLoader, properties);
		}
		return cachingProvider.getCacheManager(null, this.beanClassLoader, properties);
	}

	/**
     * Retrieves the caching provider based on the fully qualified name.
     * If the caching provider fully qualified name is provided, it returns the caching provider with the specified name.
     * If the caching provider fully qualified name is not provided, it returns the default caching provider.
     *
     * @param cachingProviderFqn the fully qualified name of the caching provider (optional)
     * @return the caching provider
     */
    private CachingProvider getCachingProvider(String cachingProviderFqn) {
		if (StringUtils.hasText(cachingProviderFqn)) {
			return Caching.getCachingProvider(cachingProviderFqn);
		}
		return Caching.getCachingProvider();
	}

	/**
     * Creates a Properties object for configuring the cache manager.
     * 
     * @param cachePropertiesCustomizers the customizers for cache properties
     * @param cacheProperties the cache properties
     * @return the properties object for configuring the cache manager
     */
    private Properties createCacheManagerProperties(
			ObjectProvider<JCachePropertiesCustomizer> cachePropertiesCustomizers, CacheProperties cacheProperties) {
		Properties properties = new Properties();
		cachePropertiesCustomizers.orderedStream()
			.forEach((customizer) -> customizer.customize(cacheProperties, properties));
		return properties;
	}

	/**
	 * Determine if JCache is available. This either kicks in if a provider is available
	 * as defined per {@link JCacheProviderAvailableCondition} or if a
	 * {@link CacheManager} has already been defined.
	 */
	@Order(Ordered.LOWEST_PRECEDENCE)
	static class JCacheAvailableCondition extends AnyNestedCondition {

		/**
         * Constructs a new JCacheAvailableCondition.
         * 
         * This condition is used to check if JCache is available.
         * 
         * @param phase the configuration phase in which this condition is registered
         */
        JCacheAvailableCondition() {
			super(ConfigurationPhase.REGISTER_BEAN);
		}

		/**
         * JCacheProvider class.
         */
        @Conditional(JCacheProviderAvailableCondition.class)
		static class JCacheProvider {

		}

		/**
         * CustomJCacheCacheManager class.
         */
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

		/**
         * Determines the outcome of the condition for the JCacheProviderAvailableCondition class.
         * 
         * @param context the condition context
         * @param metadata the annotated type metadata
         * @return the condition outcome
         */
        @Override
		public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
			ConditionMessage.Builder message = ConditionMessage.forCondition("JCache");
			String providerProperty = "spring.cache.jcache.provider";
			if (context.getEnvironment().containsProperty(providerProperty)) {
				return ConditionOutcome.match(message.because("JCache provider specified"));
			}
			Iterator<CachingProvider> providers = Caching.getCachingProviders().iterator();
			if (!providers.hasNext()) {
				return ConditionOutcome.noMatch(message.didNotFind("JSR-107 provider").atAll());
			}
			providers.next();
			if (providers.hasNext()) {
				return ConditionOutcome.noMatch(message.foundExactly("multiple JSR-107 providers"));
			}
			return ConditionOutcome.match(message.foundExactly("single JSR-107 provider"));
		}

	}

}
