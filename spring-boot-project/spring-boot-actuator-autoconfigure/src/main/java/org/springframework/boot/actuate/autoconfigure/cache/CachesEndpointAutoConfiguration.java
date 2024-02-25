/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.cache;

import java.util.Map;

import org.springframework.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnAvailableEndpoint;
import org.springframework.boot.actuate.autoconfigure.endpoint.expose.EndpointExposure;
import org.springframework.boot.actuate.cache.CachesEndpoint;
import org.springframework.boot.actuate.cache.CachesEndpointWebExtension;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link CachesEndpoint}.
 *
 * @author Johannes Edmeier
 * @author Stephane Nicoll
 * @since 2.1.0
 */
@AutoConfiguration(after = CacheAutoConfiguration.class)
@ConditionalOnClass(CacheManager.class)
@ConditionalOnAvailableEndpoint(endpoint = CachesEndpoint.class)
public class CachesEndpointAutoConfiguration {

	/**
	 * Creates a new instance of {@link CachesEndpoint} if no other bean of the same type
	 * is present.
	 * @param cacheManagers a map of cache managers
	 * @return a new instance of {@link CachesEndpoint}
	 */
	@Bean
	@ConditionalOnMissingBean
	public CachesEndpoint cachesEndpoint(Map<String, CacheManager> cacheManagers) {
		return new CachesEndpoint(cacheManagers);
	}

	/**
	 * Creates a {@link CachesEndpointWebExtension} bean if there is no existing bean of
	 * the same type. This bean is conditional on the presence of a {@link CachesEndpoint}
	 * bean and the availability of the {@link CachesEndpoint} endpoint with web and Cloud
	 * Foundry exposure.
	 * @param cachesEndpoint the {@link CachesEndpoint} bean to be used by the
	 * {@link CachesEndpointWebExtension}
	 * @return the created {@link CachesEndpointWebExtension} bean
	 */
	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnBean(CachesEndpoint.class)
	@ConditionalOnAvailableEndpoint(exposure = { EndpointExposure.WEB, EndpointExposure.CLOUD_FOUNDRY })
	public CachesEndpointWebExtension cachesEndpointWebExtension(CachesEndpoint cachesEndpoint) {
		return new CachesEndpointWebExtension(cachesEndpoint);
	}

}
