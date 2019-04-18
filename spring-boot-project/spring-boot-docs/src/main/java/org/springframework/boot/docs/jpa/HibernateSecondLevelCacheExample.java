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

package org.springframework.boot.docs.jpa;

import org.hibernate.cache.jcache.ConfigSettings;

import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.cache.jcache.JCacheCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Example configuration of using JCache and Hibernate to enable second level caching.
 *
 * @author Stephane Nicoll
 */
// tag::configuration[]
@Configuration(proxyBeanMethods = false)
public class HibernateSecondLevelCacheExample {

	@Bean
	public HibernatePropertiesCustomizer hibernateSecondLevelCacheCustomizer(
			JCacheCacheManager cacheManager) {
		return (properties) -> properties.put(ConfigSettings.CACHE_MANAGER,
				cacheManager.getCacheManager());

	}

}
// end::configuration[]
