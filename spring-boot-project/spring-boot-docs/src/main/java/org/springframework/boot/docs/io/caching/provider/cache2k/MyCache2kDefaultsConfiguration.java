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

package org.springframework.boot.docs.io.caching.provider.cache2k;

import java.util.concurrent.TimeUnit;

import org.springframework.boot.autoconfigure.cache.Cache2kBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyCache2kDefaultsConfiguration class.
 */
@Configuration(proxyBeanMethods = false)
public class MyCache2kDefaultsConfiguration {

	/**
	 * Returns a Cache2kBuilderCustomizer that sets the default configuration for Cache2k
	 * caches. The default configuration includes an entry capacity of 200 and an
	 * expiration time of 5 minutes after write.
	 * @return the Cache2kBuilderCustomizer that sets the default configuration
	 */
	@Bean
	public Cache2kBuilderCustomizer myCache2kDefaultsCustomizer() {
		// @formatter:off
		return (builder) -> builder.entryCapacity(200)
				.expireAfterWrite(5, TimeUnit.MINUTES);
		// @formatter:on
	}

}
