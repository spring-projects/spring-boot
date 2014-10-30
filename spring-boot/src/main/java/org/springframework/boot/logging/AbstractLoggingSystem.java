/*
 * Copyright 2012-2014 the original author or authors.
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
	public void initialize(String configLocation, String logFile) {
		if (StringUtils.hasLength(configLocation)) {
			// Load a specific configuration
			configLocation = SystemPropertyUtils.resolvePlaceholders(configLocation);
			loadConfiguration(configLocation, logFile);
		}
		else {
			String selfInitializationConfig = getSelfInitializationConfig();
			if (selfInitializationConfig == null) {
				// No self initialization has occurred, use defaults
				loadDefaults(logFile);
			}
			else if (StringUtils.hasLength(logFile)) {
				// Self initialization has occurred but the file has changed, reload
				loadConfiguration(selfInitializationConfig, logFile);
			}
		}
	}

	/**
	 * Return any self initialization config that has been applied. By default this method
	 * checks {@link #getStandardConfigLocations()} and assumes that any file that exists
	 * will have been applied.
	 */
	protected String getSelfInitializationConfig() {
		for (String location : getStandardConfigLocations()) {
			ClassPathResource resource = new ClassPathResource(location, this.classLoader);
			if (resource.exists()) {
				return "classpath:" + location;
			}
		}
		return null;
	}

	/**
	 * Return the standard config locations for this system.
	 * @see #getSelfInitializationConfig()
	 */
	protected abstract String[] getStandardConfigLocations();

	/**
	 * Load sensible defaults for the logging system.
	 * @param logFile the file to load or {@code null} if no log file is to be written
	 */
	protected abstract void loadDefaults(String logFile);

	/**
	 * Load a specific configuration.
	 * @param location the location of the configuration to load (never {@code null})
	 * @param logFile the file to load or {@code null} if no log file is to be written
	 */
	protected abstract void loadConfiguration(String location, String logFile);

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
