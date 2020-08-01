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
import java.io.InputStream;
import java.util.List;

import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.spring.embedded.provider.SpringEmbeddedCacheManager;

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
 * @author Raja Kolli
 * @since 1.3.0
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(SpringEmbeddedCacheManager.class)
@ConditionalOnMissingBean(CacheManager.class)
@Conditional(CacheCondition.class)
public class InfinispanCacheConfiguration {

	@Bean
	public SpringEmbeddedCacheManager cacheManager(CacheManagerCustomizers customizers,
			EmbeddedCacheManager embeddedCacheManager) {
		SpringEmbeddedCacheManager cacheManager = new SpringEmbeddedCacheManager(embeddedCacheManager);
		return customizers.customize(cacheManager);
	}

	@Bean(destroyMethod = "stop")
	@ConditionalOnMissingBean
	public EmbeddedCacheManager infinispanCacheManager(CacheProperties cacheProperties,
			ObjectProvider<ConfigurationBuilder> defaultConfigurationBuilder) throws IOException {
		EmbeddedCacheManager cacheManager = createEmbeddedCacheManager(cacheProperties);
		List<String> cacheNames = cacheProperties.getCacheNames();
		if (!CollectionUtils.isEmpty(cacheNames)) {
			cacheNames.forEach((cacheName) -> cacheManager.defineConfiguration(cacheName,
					getDefaultCacheConfiguration(defaultConfigurationBuilder.getIfAvailable())));
		}
		return cacheManager;
	}

	private EmbeddedCacheManager createEmbeddedCacheManager(CacheProperties cacheProperties) throws IOException {
		Resource location = cacheProperties.resolveConfigLocation(cacheProperties.getInfinispan().getConfig());
		if (location != null) {
			try (InputStream in = location.getInputStream()) {
				return new DefaultCacheManager(in);
			}
		}
		return new DefaultCacheManager();
	}

	private org.infinispan.configuration.cache.Configuration getDefaultCacheConfiguration(
			ConfigurationBuilder defaultConfigurationBuilder) {
		if (defaultConfigurationBuilder != null) {
			return defaultConfigurationBuilder.build();
		}
		return new ConfigurationBuilder().build();
	}

}
