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

	/**
     * Returns a HazelcastPropertiesCustomizer object that customizes the Hazelcast properties based on the provided HazelcastInstance.
     *
     * @param hazelcastInstance the HazelcastInstance object used to customize the Hazelcast properties
     * @return a HazelcastPropertiesCustomizer object that customizes the Hazelcast properties
     */
    @Bean
	HazelcastPropertiesCustomizer hazelcastPropertiesCustomizer(ObjectProvider<HazelcastInstance> hazelcastInstance) {
		return new HazelcastPropertiesCustomizer(hazelcastInstance.getIfUnique());
	}

	/**
     * HazelcastPropertiesCustomizer class.
     */
    static class HazelcastPropertiesCustomizer implements JCachePropertiesCustomizer {

		private final HazelcastInstance hazelcastInstance;

		/**
         * Constructs a new HazelcastPropertiesCustomizer with the specified HazelcastInstance.
         *
         * @param hazelcastInstance the HazelcastInstance to be used by the customizer
         */
        HazelcastPropertiesCustomizer(HazelcastInstance hazelcastInstance) {
			this.hazelcastInstance = hazelcastInstance;
		}

		/**
         * Customizes the cache properties and properties for Hazelcast.
         * 
         * @param cacheProperties the cache properties
         * @param properties the properties
         */
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

		/**
         * Converts a Resource object to a URI.
         *
         * @param config the Resource object to convert
         * @return the URI representation of the Resource object
         * @throws IllegalArgumentException if the URI cannot be obtained from the Resource object
         */
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
