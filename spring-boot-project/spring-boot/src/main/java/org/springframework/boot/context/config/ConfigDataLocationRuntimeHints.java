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

package org.springframework.boot.context.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.support.FilePatternResourceHintsRegistrar;
import org.springframework.boot.env.PropertySourceLoader;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.util.ResourceUtils;

/**
 * {@link RuntimeHintsRegistrar} implementation for application configuration.
 *
 * @author Stephane Nicoll
 * @see FilePatternResourceHintsRegistrar
 */
class ConfigDataLocationRuntimeHints implements RuntimeHintsRegistrar {

	private static final Log logger = LogFactory.getLog(ConfigDataLocationRuntimeHints.class);

	@Override
	public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
		List<String> fileNames = getFileNames(classLoader);
		List<String> locations = getLocations(classLoader);
		List<String> extensions = getExtensions(classLoader);
		if (logger.isDebugEnabled()) {
			logger.debug("Registering application configuration hints for " + fileNames + "(" + extensions + ") at "
					+ locations);
		}
		new FilePatternResourceHintsRegistrar(fileNames, locations, extensions).registerHints(hints.resources(),
				classLoader);
	}

	/**
	 * Get the application file names to consider.
	 * @param classLoader the classloader to use
	 * @return the configuration file names
	 */
	protected List<String> getFileNames(ClassLoader classLoader) {
		return Arrays.asList(StandardConfigDataLocationResolver.DEFAULT_CONFIG_NAMES);
	}

	/**
	 * Get the locations to consider. A location is a classpath location that may or may
	 * not use the standard {@code classpath:} prefix.
	 * @param classLoader the classloader to use
	 * @return the configuration file locations
	 */
	protected List<String> getLocations(ClassLoader classLoader) {
		List<String> classpathLocations = new ArrayList<>();
		for (ConfigDataLocation candidate : ConfigDataEnvironment.DEFAULT_SEARCH_LOCATIONS) {
			for (ConfigDataLocation configDataLocation : candidate.split()) {
				String location = configDataLocation.getValue();
				if (location.startsWith(ResourceUtils.CLASSPATH_URL_PREFIX)) {
					classpathLocations.add(location);
				}
			}
		}
		return classpathLocations;
	}

	/**
	 * Get the application file extensions to consider. A valid extension starts with a
	 * dot.
	 * @param classLoader the classloader to use
	 * @return the configuration file extensions
	 */
	protected List<String> getExtensions(ClassLoader classLoader) {
		List<String> extensions = new ArrayList<>();
		List<PropertySourceLoader> propertySourceLoaders = getSpringFactoriesLoader(classLoader)
				.load(PropertySourceLoader.class);
		for (PropertySourceLoader propertySourceLoader : propertySourceLoaders) {
			for (String fileExtension : propertySourceLoader.getFileExtensions()) {
				String candidate = "." + fileExtension;
				if (!extensions.contains(candidate)) {
					extensions.add(candidate);
				}
			}
		}
		return extensions;
	}

	protected SpringFactoriesLoader getSpringFactoriesLoader(ClassLoader classLoader) {
		return SpringFactoriesLoader.forDefaultResourceLocation(classLoader);
	}

}
