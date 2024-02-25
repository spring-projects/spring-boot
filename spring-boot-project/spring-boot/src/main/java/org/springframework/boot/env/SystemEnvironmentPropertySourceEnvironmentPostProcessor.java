/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.env;

import java.util.Map;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.origin.Origin;
import org.springframework.boot.origin.OriginLookup;
import org.springframework.boot.origin.SystemEnvironmentOrigin;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;
import org.springframework.util.StringUtils;

/**
 * An {@link EnvironmentPostProcessor} that replaces the systemEnvironment
 * {@link SystemEnvironmentPropertySource} with an
 * {@link OriginAwareSystemEnvironmentPropertySource} that can track the
 * {@link SystemEnvironmentOrigin} for every system environment property.
 *
 * @author Madhura Bhave
 * @since 2.0.0
 */
public class SystemEnvironmentPropertySourceEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

	/**
	 * The default order for the processor.
	 */
	public static final int DEFAULT_ORDER = SpringApplicationJsonEnvironmentPostProcessor.DEFAULT_ORDER - 1;

	private int order = DEFAULT_ORDER;

	/**
     * This method is used to post-process the environment by replacing the property source with the given environment prefix.
     * 
     * @param environment The configurable environment to be processed.
     * @param application The Spring application.
     */
    @Override
	public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
		String sourceName = StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME;
		PropertySource<?> propertySource = environment.getPropertySources().get(sourceName);
		if (propertySource != null) {
			replacePropertySource(environment, sourceName, propertySource, application.getEnvironmentPrefix());
		}
	}

	/**
     * Replaces a property source in the given environment with a new property source.
     * 
     * @param environment         the configurable environment
     * @param sourceName          the name of the property source to be replaced
     * @param propertySource      the new property source to be added
     * @param environmentPrefix   the prefix for environment variables
     */
    @SuppressWarnings("unchecked")
	private void replacePropertySource(ConfigurableEnvironment environment, String sourceName,
			PropertySource<?> propertySource, String environmentPrefix) {
		Map<String, Object> originalSource = (Map<String, Object>) propertySource.getSource();
		SystemEnvironmentPropertySource source = new OriginAwareSystemEnvironmentPropertySource(sourceName,
				originalSource, environmentPrefix);
		environment.getPropertySources().replace(sourceName, source);
	}

	/**
     * Returns the order of this SystemEnvironmentPropertySourceEnvironmentPostProcessor.
     * 
     * @return the order of this SystemEnvironmentPropertySourceEnvironmentPostProcessor
     */
    @Override
	public int getOrder() {
		return this.order;
	}

	/**
     * Sets the order of the SystemEnvironmentPropertySourceEnvironmentPostProcessor.
     * 
     * @param order the order to set
     */
    public void setOrder(int order) {
		this.order = order;
	}

	/**
	 * {@link SystemEnvironmentPropertySource} that also tracks {@link Origin}.
	 */
	protected static class OriginAwareSystemEnvironmentPropertySource extends SystemEnvironmentPropertySource
			implements OriginLookup<String> {

		private final String prefix;

		/**
         * Constructs a new OriginAwareSystemEnvironmentPropertySource with the specified name, source, and environment prefix.
         * 
         * @param name the name of the property source
         * @param source the source map containing the properties
         * @param environmentPrefix the prefix to be used for environment-specific properties
         */
        OriginAwareSystemEnvironmentPropertySource(String name, Map<String, Object> source, String environmentPrefix) {
			super(name, source);
			this.prefix = determinePrefix(environmentPrefix);
		}

		/**
         * Determines the prefix for the given environment prefix.
         * 
         * @param environmentPrefix the environment prefix to determine the prefix for
         * @return the determined prefix, or null if the environment prefix is empty or null
         */
        private String determinePrefix(String environmentPrefix) {
			if (!StringUtils.hasText(environmentPrefix)) {
				return null;
			}
			if (environmentPrefix.endsWith(".") || environmentPrefix.endsWith("_") || environmentPrefix.endsWith("-")) {
				return environmentPrefix.substring(0, environmentPrefix.length() - 1);
			}
			return environmentPrefix;
		}

		/**
         * Checks if the property source contains a property with the given name.
         *
         * @param name the name of the property to check
         * @return true if the property source contains the property, false otherwise
         */
        @Override
		public boolean containsProperty(String name) {
			return super.containsProperty(name);
		}

		/**
         * Retrieves the value of the specified property from the system environment.
         * 
         * @param name the name of the property to retrieve
         * @return the value of the property, or null if the property does not exist
         */
        @Override
		public Object getProperty(String name) {
			return super.getProperty(name);
		}

		/**
         * Retrieves the origin of a property value based on the provided key.
         * 
         * @param key the key of the property
         * @return the origin of the property value, or null if the property does not exist
         */
        @Override
		public Origin getOrigin(String key) {
			String property = resolvePropertyName(key);
			if (super.containsProperty(property)) {
				return new SystemEnvironmentOrigin(property);
			}
			return null;
		}

		/**
         * Returns the prefix of the OriginAwareSystemEnvironmentPropertySource.
         *
         * @return the prefix of the OriginAwareSystemEnvironmentPropertySource
         */
        @Override
		public String getPrefix() {
			return this.prefix;
		}

	}

}
