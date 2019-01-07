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

package org.springframework.boot.actuate.autoconfigure.cache;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnEnabledEndpoint;
import org.springframework.boot.actuate.cache.CachesEndpoint;
import org.springframework.boot.actuate.cache.CachesEndpointWebExtension;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link CachesEndpoint}.
 *
 * @author Johannes Edmeier
 * @author Stephane Nicoll
 * @since 2.1.0
 */
@Configuration
@ConditionalOnClass(CacheManager.class)
@ConditionalOnEnabledEndpoint(endpoint = CachesEndpoint.class)
@AutoConfigureAfter(CacheAutoConfiguration.class)
public class CachesEndpointAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public CachesEndpoint cachesEndpoint(
			ObjectProvider<Map<String, CacheManager>> cacheManagers) {
		return new CachesEndpoint(cacheManagers.getIfAvailable(LinkedHashMap::new));
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnBean(CachesEndpoint.class)
	public CachesEndpointWebExtension cachesEndpointWebExtension(
			CachesEndpoint cachesEndpoint) {
		return new CachesEndpointWebExtension(cachesEndpoint);
	}

}
