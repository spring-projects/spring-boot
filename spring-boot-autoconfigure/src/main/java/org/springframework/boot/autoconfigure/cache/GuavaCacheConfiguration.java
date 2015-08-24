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

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cache.CacheManager;
import org.springframework.cache.guava.GuavaCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheBuilderSpec;
import com.google.common.cache.CacheLoader;

/**
 * Guava cache configuration.
 *
 * @author Stephane Nicoll
 * @since 1.3.0
 */
@Configuration
@ConditionalOnClass({ CacheBuilder.class, GuavaCacheManager.class })
@ConditionalOnMissingBean(CacheManager.class)
@Conditional(CacheCondition.class)
class GuavaCacheConfiguration {

	@Autowired
	private CacheProperties cacheProperties;

	@Autowired(required = false)
	private CacheBuilder<Object, Object> cacheBuilder;

	@Autowired(required = false)
	private CacheBuilderSpec cacheBuilderSpec;

	@Autowired(required = false)
	private CacheLoader<Object, Object> cacheLoader;

	@Bean
	public GuavaCacheManager cacheManager() {
		GuavaCacheManager cacheManager = createCacheManager();
		List<String> cacheNames = this.cacheProperties.getCacheNames();
		if (!CollectionUtils.isEmpty(cacheNames)) {
			cacheManager.setCacheNames(cacheNames);
		}
		return cacheManager;
	}

	private GuavaCacheManager createCacheManager() {
		GuavaCacheManager cacheManager = new GuavaCacheManager();
		setCacheBuilder(cacheManager);
		if (this.cacheLoader != null) {
			cacheManager.setCacheLoader(this.cacheLoader);
		}
		return cacheManager;
	}

	private void setCacheBuilder(GuavaCacheManager cacheManager) {
		String specification = this.cacheProperties.getGuava().getSpec();
		if (StringUtils.hasText(specification)) {
			cacheManager.setCacheSpecification(specification);
		}
		else if (this.cacheBuilderSpec != null) {
			cacheManager.setCacheBuilderSpec(this.cacheBuilderSpec);
		}
		else if (this.cacheBuilder != null) {
			cacheManager.setCacheBuilder(this.cacheBuilder);
		}
	}

}
