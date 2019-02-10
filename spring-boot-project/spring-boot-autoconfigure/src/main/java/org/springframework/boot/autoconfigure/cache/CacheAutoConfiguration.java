/*
 * Copyright 2012-2019 the original author or authors.
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

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration.CacheConfigurationImportSelector;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.couchbase.CouchbaseAutoConfiguration;
import org.springframework.boot.autoconfigure.data.jpa.EntityManagerFactoryDependsOnPostProcessor;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.hazelcast.HazelcastAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheAspectSupport;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.orm.jpa.AbstractEntityManagerFactoryBean;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for the cache abstraction. Creates a
 * {@link CacheManager} if necessary when caching is enabled via {@link EnableCaching}.
 * <p>
 * Cache store can be auto-detected or specified explicitly via configuration.
 *
 * @author Stephane Nicoll
 * @since 1.3.0
 * @see EnableCaching
 */
@Configuration
@ConditionalOnClass(CacheManager.class)
@ConditionalOnBean(CacheAspectSupport.class)
@ConditionalOnMissingBean(value = CacheManager.class, name = "cacheResolver")
@EnableConfigurationProperties(CacheProperties.class)
@AutoConfigureAfter({ CouchbaseAutoConfiguration.class, HazelcastAutoConfiguration.class,
		HibernateJpaAutoConfiguration.class, RedisAutoConfiguration.class })
@Import(CacheConfigurationImportSelector.class)
public class CacheAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public CacheManagerCustomizers cacheManagerCustomizers(
			ObjectProvider<CacheManagerCustomizer<?>> customizers) {
		return new CacheManagerCustomizers(
				customizers.orderedStream().collect(Collectors.toList()));
	}

	@Bean
	public CacheManagerValidator cacheAutoConfigurationValidator(
			CacheProperties cacheProperties,
			ObjectProvider<Map<String, CacheManager>> cacheManagers) {
		return new CacheManagerValidator(cacheProperties, cacheManagers);
	}

	@Configuration
	@ConditionalOnClass(LocalContainerEntityManagerFactoryBean.class)
	@ConditionalOnBean(AbstractEntityManagerFactoryBean.class)
	protected static class CacheManagerJpaDependencyConfiguration
			extends EntityManagerFactoryDependsOnPostProcessor {

		public CacheManagerJpaDependencyConfiguration() {
			super("cacheManager");
		}

	}

	/**
	 * Bean used to validate that a CacheManager exists and it unique.
	 */
	static class CacheManagerValidator implements InitializingBean {

		private final CacheProperties cacheProperties;

		private final Map<String, CacheManager> cacheManagers;

		CacheManagerValidator(CacheProperties cacheProperties,
				ObjectProvider<Map<String, CacheManager>> cacheManagers) {
			this.cacheProperties = cacheProperties;
			this.cacheManagers = cacheManagers.getIfAvailable();
		}

		@Override
		public void afterPropertiesSet() {
			if (CollectionUtils.isEmpty(this.cacheManagers)) {
				throw new NoSuchCacheManagerException(this.cacheProperties);
			}
			if (this.cacheManagers.size() > 1) {
				throw new NoUniqueCacheManagerException(this.cacheManagers.keySet(),
						this.cacheProperties);
			}
		}

	}

	/**
	 * {@link ImportSelector} to add {@link CacheType} configuration classes.
	 */
	static class CacheConfigurationImportSelector implements ImportSelector {

		@Override
		public String[] selectImports(AnnotationMetadata importingClassMetadata) {
			CacheType[] types = CacheType.values();
			String[] imports = new String[types.length];
			for (int i = 0; i < types.length; i++) {
				imports[i] = CacheConfigurations.getConfigurationClass(types[i]);
			}
			return imports;
		}

	}

	/**
	 * Exception thrown when {@link org.springframework.cache.CacheManager} implementation
	 * are not specified.
	 */
	static class NoSuchCacheManagerException extends RuntimeException {

		private final CacheProperties properties;

		NoSuchCacheManagerException(CacheProperties properties) {
			super(String.format("No qualifying bean of type '%s' available",
					CacheManager.class.getName()));
			this.properties = properties;
		}

		NoSuchCacheManagerException(String message, CacheProperties properties) {
			super(message);
			this.properties = properties;
		}

		CacheProperties getProperties() {
			return this.properties;
		}

	}

	/**
	 * Exception thrown when multiple {@link org.springframework.cache.CacheManager}
	 * implementations are available with no way to know which implementation should be
	 * used.
	 */
	static class NoUniqueCacheManagerException extends NoSuchCacheManagerException {

		private final Set<String> beanNames;

		NoUniqueCacheManagerException(Set<String> beanNames, CacheProperties properties) {
			super(String.format(
					"expected single matching bean of type '%s' but found " + "%d: %s",
					CacheManager.class.getName(), beanNames.size(),
					StringUtils.collectionToCommaDelimitedString(beanNames)), properties);
			this.beanNames = beanNames;
		}

		Set<String> getBeanNames() {
			return this.beanNames;
		}

	}

}
