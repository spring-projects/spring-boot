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

package org.springframework.boot.logging;

import org.springframework.core.io.ClassPathResource;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.SystemPropertyUtils;

/**
 * Abstract base class for {@link LoggingSystem} implementations.
 *
 * @author Phillip Webb
 * @author Dave Syer
 */
public abstract class AbstractLoggingSystem extends LoggingSystem {

	private final ClassLoader classLoader;

	public AbstractLoggingSystem(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}

	@Override
	public void beforeInitialize() {
	}

	@Override
	public void initialize(String configLocation, LogFile logFile) {
		if (StringUtils.hasLength(configLocation)) {
			initializeWithSpecificConfig(configLocation, logFile);
			return;
		}
		initializeWithConventions(logFile);
	}

	private void initializeWithSpecificConfig(String configLocation, LogFile logFile) {
		configLocation = SystemPropertyUtils.resolvePlaceholders(configLocation);
		loadConfiguration(configLocation, logFile);
	}

	private void initializeWithConventions(LogFile logFile) {
		String config = getSelfInitializationConfig();
		if (config != null && logFile == null) {
			// self initialization has occurred, reinitialize in case of property changes
			reinitialize();
			return;
		}
		if (config == null) {
			config = getSpringInitializationConfig();
		}
		if (config != null) {
			loadConfiguration(config, logFile);
			return;
		}
		loadDefaults(logFile);
	}

	/**
	 * Return any self initialization config that has been applied. By default this method
	 * checks {@link #getStandardConfigLocations()} and assumes that any file that exists
	 * will have been applied.
	 * @return the self initialization configor {@code null}
	 */
	protected String getSelfInitializationConfig() {
		return findConfig(getStandardConfigLocations());
	}

	/**
	 * Return any spring specific initialization config that should be applied. By default
	 * this method checks {@link #getSpringConfigLocations()}.
	 * @return the spring initialization config or {@code null}
	 */
	protected String getSpringInitializationConfig() {
		return findConfig(getSpringConfigLocations());
	}

	private String findConfig(String[] locations) {
		for (String location : locations) {
			ClassPathResource resource = new ClassPathResource(location, this.classLoader);
			if (resource.exists()) {
				return "classpath:" + location;
			}
		}
		return null;
	}

	/**
	 * Return the standard config locations for this system.
	 * @return the standard config locations
	 * @see #getSelfInitializationConfig()
	 */
	protected abstract String[] getStandardConfigLocations();

	/**
	 * Return the spring config locations for this system. By default this method returns
	 * a set of locations based on {@link #getStandardConfigLocations()}.
	 * @return the standard config locations
	 * @see #getSpringInitializationConfig()
	 */
	protected String[] getSpringConfigLocations() {
		String[] locations = getStandardConfigLocations();
		for (int i = 0; i < locations.length; i++) {
			String extension = StringUtils.getFilenameExtension(locations[i]);
			locations[i] = locations[i].substring(0,
					locations[i].length() - extension.length() - 1)
					+ "-spring." + extension;
		}
		return locations;
	}

	/**
	 * Load sensible defaults for the logging system.
	 * @param logFile the file to load or {@code null} if no log file is to be written
	 */
	protected abstract void loadDefaults(LogFile logFile);

	/**
	 * Load a specific configuration.
	 * @param location the location of the configuration to load (never {@code null})
	 * @param logFile the file to load or {@code null} if no log file is to be written
	 */
	protected abstract void loadConfiguration(String location, LogFile logFile);

	/**
	 * Reinitialize the logging system if required. Called when
	 * {@link #getSelfInitializationConfig()} is used and the log file hasn't changed. May
	 * be used to reload configuration (for example to pickup additional System
	 * properties).
	 */
	protected void reinitialize() {
	}

	protected final ClassLoader getClassLoader() {
		return this.classLoader;
	}

	protected final String getPackagedConfigFile(String fileName) {
		String defaultPath = ClassUtils.getPackageName(getClass());
		defaultPath = defaultPath.replace(".", "/");
		defaultPath = defaultPath + "/" + fileName;
		defaultPath = "classpath:" + defaultPath;
		return defaultPath;
	}

}
