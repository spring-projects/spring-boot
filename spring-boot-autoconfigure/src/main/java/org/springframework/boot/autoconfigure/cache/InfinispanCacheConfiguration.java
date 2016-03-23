/*
 * Copyright 2012-2016 the original author or authors.
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
import java.io.InputStream;
import java.util.List;

import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.spring.provider.SpringEmbeddedCacheManager;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.util.CollectionUtils;

/**
 * Infinispan cache configuration.
 *
 * @author Eddú Meléndez
 * @author Stephane Nicoll
 * @since 1.3.0
 */
@Configuration
@ConditionalOnClass(SpringEmbeddedCacheManager.class)
@ConditionalOnMissingBean(CacheManager.class)
@Conditional(CacheCondition.class)
public class InfinispanCacheConfiguration {

	private final CacheProperties cacheProperties;

	private final CacheManagerCustomizers customizers;

	private final ConfigurationBuilder defaultConfigurationBuilder;

	public InfinispanCacheConfiguration(CacheProperties cacheProperties,
			CacheManagerCustomizers customizers,
			ObjectProvider<ConfigurationBuilder> defaultConfigurationBuilderProvider) {
		this.cacheProperties = cacheProperties;
		this.customizers = customizers;
		this.defaultConfigurationBuilder = defaultConfigurationBuilderProvider
				.getIfAvailable();
	}

	@Bean
	public SpringEmbeddedCacheManager cacheManager(
			EmbeddedCacheManager embeddedCacheManager) {
		SpringEmbeddedCacheManager cacheManager = new SpringEmbeddedCacheManager(
				embeddedCacheManager);
		return this.customizers.customize(cacheManager);
	}

	@Bean(destroyMethod = "stop")
	@ConditionalOnMissingBean
	public EmbeddedCacheManager infinispanCacheManager() throws IOException {
		EmbeddedCacheManager cacheManager = createEmbeddedCacheManager();
		List<String> cacheNames = this.cacheProperties.getCacheNames();
		if (!CollectionUtils.isEmpty(cacheNames)) {
			for (String cacheName : cacheNames) {
				cacheManager.defineConfiguration(cacheName,
						getDefaultCacheConfiguration());
			}
		}
		return cacheManager;
	}

	private EmbeddedCacheManager createEmbeddedCacheManager() throws IOException {
		Resource location = this.cacheProperties
				.resolveConfigLocation(this.cacheProperties.getInfinispan().getConfig());
		if (location != null) {
			InputStream in = location.getInputStream();
			try {
				return new DefaultCacheManager(in);
			}
			finally {
				in.close();
			}
		}
		return new DefaultCacheManager();
	}

	private org.infinispan.configuration.cache.Configuration getDefaultCacheConfiguration() {
		if (this.defaultConfigurationBuilder != null) {
			return this.defaultConfigurationBuilder.build();
		}
		return new ConfigurationBuilder().build();
	}

}
