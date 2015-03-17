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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Make sure to throw a dedicated exception message if no cache manager could
 * have been configured. Note that this configuration class is imported if
 * {@code EnableCaching} has been set and no {@link CacheManager} could have
 * been auto-configured.
 *
 * @author Stephane Nicoll
 * @since 1.3.0
 */
@Configuration
@ConditionalOnMissingBean(CacheManager.class)
class CacheValidationConfiguration {

	@Autowired
	private CacheProperties cacheProperties;

	@Bean
	public CacheManager cacheManager() {
		throw new IllegalStateException("No cache manager could be auto-configured, check your " +
				"configuration (caching mode is '" + this.cacheProperties.getMode() + "')");
	}

}
