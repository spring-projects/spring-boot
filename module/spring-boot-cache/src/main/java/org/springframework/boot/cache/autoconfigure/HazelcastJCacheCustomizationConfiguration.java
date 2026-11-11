/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.cache.autoconfigure;

import java.io.IOException;
import java.net.URI;
import java.util.Properties;

import com.hazelcast.core.HazelcastInstance;
import org.jspecify.annotations.Nullable;

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
	HazelcastPropertiesCustomizer hazelcastPropertiesCustomizer(ObjectProvider<HazelcastInstance> hazelcastInstance,
			CacheProperties cacheProperties) {
		return new HazelcastPropertiesCustomizer(hazelcastInstance.getIfUnique(), cacheProperties);
	}

	static class HazelcastPropertiesCustomizer implements JCachePropertiesCustomizer {

		private final @Nullable HazelcastInstance hazelcastInstance;

		private final CacheProperties cacheProperties;

		HazelcastPropertiesCustomizer(@Nullable HazelcastInstance hazelcastInstance, CacheProperties cacheProperties) {
			this.hazelcastInstance = hazelcastInstance;
			this.cacheProperties = cacheProperties;
		}

		@Override
		public void customize(Properties properties) {
			Resource configLocation = this.cacheProperties
				.resolveConfigLocation(this.cacheProperties.getJcache().getConfig());
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
