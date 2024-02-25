/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.autoconfigure.integration;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.boot.env.OriginTrackedMapPropertySource;
import org.springframework.boot.env.PropertiesPropertySourceLoader;
import org.springframework.boot.origin.Origin;
import org.springframework.boot.origin.OriginLookup;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.integration.context.IntegrationProperties;

/**
 * An {@link EnvironmentPostProcessor} that maps the configuration of
 * {@code META-INF/spring.integration.properties} in the environment.
 *
 * @author Artem Bilan
 * @author Stephane Nicoll
 */
class IntegrationPropertiesEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

	/**
     * Returns the order value of this post-processor.
     * 
     * The order value is set to {@link Ordered#LOWEST_PRECEDENCE}, indicating that this post-processor
     * should be executed after all other post-processors.
     * 
     * @return the order value of this post-processor
     */
    @Override
	public int getOrder() {
		return Ordered.LOWEST_PRECEDENCE;
	}

	/**
     * Post-processes the environment by registering a property source for the integration properties file.
     * 
     * @param environment the configurable environment
     * @param application the spring application
     */
    @Override
	public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
		Resource resource = new ClassPathResource("META-INF/spring.integration.properties");
		if (resource.exists()) {
			registerIntegrationPropertiesPropertySource(environment, resource);
		}
	}

	/**
     * Registers the integration properties property source in the given environment.
     * 
     * @param environment the configurable environment
     * @param resource the resource containing the integration properties
     * @throws IllegalStateException if failed to load integration properties from the resource
     */
    protected void registerIntegrationPropertiesPropertySource(ConfigurableEnvironment environment, Resource resource) {
		PropertiesPropertySourceLoader loader = new PropertiesPropertySourceLoader();
		try {
			OriginTrackedMapPropertySource propertyFileSource = (OriginTrackedMapPropertySource) loader
				.load("META-INF/spring.integration.properties", resource)
				.get(0);
			environment.getPropertySources().addLast(new IntegrationPropertiesPropertySource(propertyFileSource));
		}
		catch (IOException ex) {
			throw new IllegalStateException("Failed to load integration properties from " + resource, ex);
		}
	}

	/**
     * IntegrationPropertiesPropertySource class.
     */
    private static final class IntegrationPropertiesPropertySource extends PropertySource<Map<String, Object>>
			implements OriginLookup<String> {

		private static final String PREFIX = "spring.integration.";

		private static final Map<String, String> KEYS_MAPPING;

		static {
			Map<String, String> mappings = new HashMap<>();
			mappings.put(PREFIX + "channel.auto-create", IntegrationProperties.CHANNELS_AUTOCREATE);
			mappings.put(PREFIX + "channel.max-unicast-subscribers",
					IntegrationProperties.CHANNELS_MAX_UNICAST_SUBSCRIBERS);
			mappings.put(PREFIX + "channel.max-broadcast-subscribers",
					IntegrationProperties.CHANNELS_MAX_BROADCAST_SUBSCRIBERS);
			mappings.put(PREFIX + "error.require-subscribers", IntegrationProperties.ERROR_CHANNEL_REQUIRE_SUBSCRIBERS);
			mappings.put(PREFIX + "error.ignore-failures", IntegrationProperties.ERROR_CHANNEL_IGNORE_FAILURES);
			mappings.put(PREFIX + "endpoint.throw-exception-on-late-reply",
					IntegrationProperties.THROW_EXCEPTION_ON_LATE_REPLY);
			mappings.put(PREFIX + "endpoint.read-only-headers", IntegrationProperties.READ_ONLY_HEADERS);
			mappings.put(PREFIX + "endpoint.no-auto-startup", IntegrationProperties.ENDPOINTS_NO_AUTO_STARTUP);
			KEYS_MAPPING = Collections.unmodifiableMap(mappings);
		}

		private final OriginTrackedMapPropertySource delegate;

		/**
         * Constructs a new IntegrationPropertiesPropertySource with the specified delegate.
         * 
         * @param delegate the OriginTrackedMapPropertySource delegate
         */
        IntegrationPropertiesPropertySource(OriginTrackedMapPropertySource delegate) {
			super("META-INF/spring.integration.properties", delegate.getSource());
			this.delegate = delegate;
		}

		/**
         * Retrieves the value of the specified property.
         * 
         * @param name the name of the property to retrieve
         * @return the value of the property
         */
        @Override
		public Object getProperty(String name) {
			return this.delegate.getProperty(KEYS_MAPPING.get(name));
		}

		/**
         * Retrieves the origin of a property value based on the given key.
         * 
         * @param key the key of the property
         * @return the origin of the property value
         */
        @Override
		public Origin getOrigin(String key) {
			return this.delegate.getOrigin(KEYS_MAPPING.get(key));
		}

	}

}
