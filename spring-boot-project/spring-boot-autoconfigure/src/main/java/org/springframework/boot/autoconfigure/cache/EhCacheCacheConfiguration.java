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

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ResourceCondition;
import org.springframework.cache.ehcache.EhCacheCacheManager;
import org.springframework.cache.ehcache.EhCacheManagerUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

/**
 * EhCache cache configuration. Only kick in if a configuration file location is set or if
 * a default configuration file exists.
 *
 * @author Eddú Meléndez
 * @author Stephane Nicoll
 * @author Madhura Bhave
 * @since 1.3.0
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({ Cache.class, EhCacheCacheManager.class })
@ConditionalOnMissingBean(org.springframework.cache.CacheManager.class)
@Conditional({ CacheCondition.class, EhCacheCacheConfiguration.ConfigAvailableCondition.class })
class EhCacheCacheConfiguration {

	@Bean
	public EhCacheCacheManager cacheManager(CacheManagerCustomizers customizers, CacheManager ehCacheCacheManager) {
		return customizers.customize(new EhCacheCacheManager(ehCacheCacheManager));
	}

	@Bean
	@ConditionalOnMissingBean
	public CacheManager ehCacheCacheManager(CacheProperties cacheProperties) {
		Resource location = cacheProperties.resolveConfigLocation(cacheProperties.getEhcache().getConfig());
		if (location != null) {
			return EhCacheManagerUtils.buildCacheManager(location);
		}
		return EhCacheManagerUtils.buildCacheManager();
	}

	/**
	 * Determine if the EhCache configuration is available. This either kick in if a
	 * default configuration has been found or if property referring to the file to use
	 * has been set.
	 */
	static class ConfigAvailableCondition extends ResourceCondition {

		ConfigAvailableCondition() {
			super("EhCache", "spring.cache.ehcache.config", "classpath:/ehcache.xml");
		}

	}

}
