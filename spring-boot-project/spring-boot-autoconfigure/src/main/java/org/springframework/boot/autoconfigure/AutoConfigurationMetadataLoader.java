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

package org.springframework.boot.autoconfigure;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Set;

import org.springframework.core.io.UrlResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.util.StringUtils;

/**
 * Internal utility used to load {@link AutoConfigurationMetadata}.
 *
 * @author Phillip Webb
 */
final class AutoConfigurationMetadataLoader {

	protected static final String PATH = "META-INF/spring-autoconfigure-metadata.properties";

	/**
     * Constructs a new AutoConfigurationMetadataLoader.
     */
    private AutoConfigurationMetadataLoader() {
	}

	/**
     * Loads the auto-configuration metadata using the specified class loader and default path.
     * 
     * @param classLoader the class loader to use for loading the metadata
     * @return the auto-configuration metadata
     */
    static AutoConfigurationMetadata loadMetadata(ClassLoader classLoader) {
		return loadMetadata(classLoader, PATH);
	}

	/**
     * Loads the metadata for auto-configuration from the specified path.
     * 
     * @param classLoader the class loader to use for loading resources
     * @param path the path to the resources containing the metadata
     * @return the loaded auto-configuration metadata
     * @throws IllegalArgumentException if unable to load the metadata
     */
    static AutoConfigurationMetadata loadMetadata(ClassLoader classLoader, String path) {
		try {
			Enumeration<URL> urls = (classLoader != null) ? classLoader.getResources(path)
					: ClassLoader.getSystemResources(path);
			Properties properties = new Properties();
			while (urls.hasMoreElements()) {
				properties.putAll(PropertiesLoaderUtils.loadProperties(new UrlResource(urls.nextElement())));
			}
			return loadMetadata(properties);
		}
		catch (IOException ex) {
			throw new IllegalArgumentException("Unable to load @ConditionalOnClass location [" + path + "]", ex);
		}
	}

	/**
     * Loads the metadata for auto-configuration from the given properties.
     * 
     * @param properties the properties containing the metadata
     * @return the auto-configuration metadata
     */
    static AutoConfigurationMetadata loadMetadata(Properties properties) {
		return new PropertiesAutoConfigurationMetadata(properties);
	}

	/**
	 * {@link AutoConfigurationMetadata} implementation backed by a properties file.
	 */
	private static class PropertiesAutoConfigurationMetadata implements AutoConfigurationMetadata {

		private final Properties properties;

		/**
         * Constructs a new instance of PropertiesAutoConfigurationMetadata with the specified properties.
         *
         * @param properties the properties to be used for configuration metadata
         */
        PropertiesAutoConfigurationMetadata(Properties properties) {
			this.properties = properties;
		}

		/**
         * Checks if the specified class name was processed.
         * 
         * @param className the name of the class to check
         * @return true if the class name was processed, false otherwise
         */
        @Override
		public boolean wasProcessed(String className) {
			return this.properties.containsKey(className);
		}

		/**
         * Retrieves the value associated with the specified key as an Integer from the given class name.
         * If the key is not found, the default value provided is returned.
         *
         * @param className the name of the class to retrieve the value from
         * @param key the key to retrieve the value for
         * @return the value associated with the key as an Integer, or the default value if the key is not found
         */
        @Override
		public Integer getInteger(String className, String key) {
			return getInteger(className, key, null);
		}

		/**
         * Retrieves the value associated with the specified key in the given class name.
         * If the value is not found, the default value is returned.
         * 
         * @param className the name of the class to retrieve the value from
         * @param key the key associated with the value
         * @param defaultValue the default value to return if the value is not found
         * @return the value associated with the key, or the default value if not found
         */
        @Override
		public Integer getInteger(String className, String key, Integer defaultValue) {
			String value = get(className, key);
			return (value != null) ? Integer.valueOf(value) : defaultValue;
		}

		/**
         * Retrieves a set of values from the specified class and key.
         * 
         * @param className the name of the class to retrieve the set from
         * @param key the key to retrieve the set of values for
         * @return a set of values retrieved from the specified class and key
         */
        @Override
		public Set<String> getSet(String className, String key) {
			return getSet(className, key, null);
		}

		/**
         * Retrieves a set of values from the properties file based on the given class name and key.
         * 
         * @param className the name of the class to retrieve the properties from
         * @param key the key of the property to retrieve
         * @param defaultValue the default set of values to return if the property is not found
         * @return a set of values retrieved from the properties file, or the default set of values if the property is not found
         */
        @Override
		public Set<String> getSet(String className, String key, Set<String> defaultValue) {
			String value = get(className, key);
			return (value != null) ? StringUtils.commaDelimitedListToSet(value) : defaultValue;
		}

		/**
         * Retrieves the value associated with the specified key from the given class name.
         * If the key is not found, returns the default value.
         *
         * @param className the name of the class to retrieve the value from
         * @param key the key to retrieve the value for
         * @return the value associated with the key, or the default value if not found
         */
        @Override
		public String get(String className, String key) {
			return get(className, key, null);
		}

		/**
         * Retrieves the value associated with the specified key in the given class name's properties.
         * If the value is not found, the default value is returned instead.
         *
         * @param className    the name of the class
         * @param key          the key to retrieve the value for
         * @param defaultValue the default value to return if the value is not found
         * @return the value associated with the key, or the default value if not found
         */
        @Override
		public String get(String className, String key, String defaultValue) {
			String value = this.properties.getProperty(className + "." + key);
			return (value != null) ? value : defaultValue;
		}

	}

}
