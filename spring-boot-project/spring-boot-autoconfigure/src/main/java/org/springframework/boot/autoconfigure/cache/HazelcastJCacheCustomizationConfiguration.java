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
import java.net.URI;
import java.util.Properties;

import com.hazelcast.core.HazelcastInstance;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

/**
 * JCache customization for Hazelcast.
 *
 * @author Stephane Nicoll
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(HazelcastInstance.class)
class HazelcastJCacheCustomizationConfiguration {

	@Bean
	HazelcastPropertiesCustomizer hazelcastPropertiesCustomizer(ObjectProvider<HazelcastInstance> hazelcastInstance) {
		return new HazelcastPropertiesCustomizer(hazelcastInstance.getIfUnique());
	}

	static class HazelcastPropertiesCustomizer implements JCachePropertiesCustomizer {

		private final HazelcastInstance hazelcastInstance;

		HazelcastPropertiesCustomizer(HazelcastInstance hazelcastInstance) {
			this.hazelcastInstance = hazelcastInstance;
		}

		@Override
		public void customize(CacheProperties cacheProperties, Properties properties) {
			Resource configLocation = cacheProperties.resolveConfigLocation(cacheProperties.getJcache().getConfig());
			if (configLocation != null) {
				// Hazelcast does not use the URI as a mean to specify a custom config.
				properties.setProperty("hazelcast.config.location", toUri(configLocation).toString());
			}
			else if (this.hazelcastInstance != null) {
				properties.put("hazelcast.instance.itself", this.hazelcastInstance);
			}
		}

		private static URI toUri(Resource config) {
			try {
				return config.getURI();
			}
			catch (IOException ex) {
				throw new IllegalArgumentException("Could not get URI from " + config, ex);
			}
		}

	}

}
