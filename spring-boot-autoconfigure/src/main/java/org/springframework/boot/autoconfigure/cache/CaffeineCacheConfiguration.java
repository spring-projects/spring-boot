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

import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.CaffeineSpec;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * Caffeine cache configuration.
 *
 * @author Eddú Meléndez
 * @since 1.4.0
 */
@Configuration
@ConditionalOnClass({ Caffeine.class, CaffeineCacheManager.class })
@ConditionalOnMissingBean(org.springframework.cache.CacheManager.class)
@Conditional({ CacheCondition.class })
class CaffeineCacheConfiguration {

	@Autowired
	private CacheProperties cacheProperties;

	@Autowired(required = false)
	private Caffeine<Object, Object> caffeine;

	@Autowired(required = false)
	private CaffeineSpec caffeineSpec;

	@Autowired(required = false)
	private CacheLoader<Object, Object> cacheLoader;

	@Bean
	@ConditionalOnMissingBean
	public CaffeineCacheManager caffeineCacheManager() {
		CaffeineCacheManager caffeineCacheManager = new CaffeineCacheManager();
		setCacheBuilder(caffeineCacheManager);
		if (this.cacheLoader != null) {
			caffeineCacheManager.setCacheLoader(this.cacheLoader);
		}
		caffeineCacheManager.setCacheNames(this.cacheProperties.getCacheNames());
		return caffeineCacheManager;
	}

	private void setCacheBuilder(CaffeineCacheManager caffeineCacheManager) {
		String specification = this.cacheProperties.getCaffeine().getSpec();
		if (StringUtils.hasText(specification)) {
			caffeineCacheManager.setCaffeine(Caffeine.from(specification));
		}
		else if (this.caffeineSpec != null) {
			caffeineCacheManager.setCaffeine(Caffeine.from(this.caffeineSpec));
		}
		else if (this.caffeine != null) {
			caffeineCacheManager.setCaffeine(this.caffeine);
		}
	}

}
