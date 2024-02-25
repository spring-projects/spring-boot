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

package org.springframework.boot.logging;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.SystemPropertyUtils;

/**
 * Abstract base class for {@link LoggingSystem} implementations.
 *
 * @author Phillip Webb
 * @author Dave Syer
 * @since 1.0.0
 */
public abstract class AbstractLoggingSystem extends LoggingSystem {

	protected static final Comparator<LoggerConfiguration> CONFIGURATION_COMPARATOR = new LoggerConfigurationComparator(
			ROOT_LOGGER_NAME);

	private final ClassLoader classLoader;

	/**
	 * Constructs a new AbstractLoggingSystem with the specified class loader.
	 * @param classLoader the class loader to be used by the logging system
	 */
	public AbstractLoggingSystem(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}

	/**
	 * This method is called before the initialization of the logging system.
	 */
	@Override
	public void beforeInitialize() {
	}

	/**
	 * Initializes the logging system.
	 * @param initializationContext the logging initialization context
	 * @param configLocation the location of the logging configuration file
	 * @param logFile the log file to be used
	 */
	@Override
	public void initialize(LoggingInitializationContext initializationContext, String configLocation, LogFile logFile) {
		if (StringUtils.hasLength(configLocation)) {
			initializeWithSpecificConfig(initializationContext, configLocation, logFile);
			return;
		}
		initializeWithConventions(initializationContext, logFile);
	}

	/**
	 * Initializes the logging system with a specific configuration.
	 * @param initializationContext the context for logging initialization
	 * @param configLocation the location of the configuration file
	 * @param logFile the log file to be used
	 */
	private void initializeWithSpecificConfig(LoggingInitializationContext initializationContext, String configLocation,
			LogFile logFile) {
		configLocation = SystemPropertyUtils.resolvePlaceholders(configLocation);
		loadConfiguration(initializationContext, configLocation, logFile);
	}

	/**
	 * Initializes the logging system with conventions.
	 * @param initializationContext the logging initialization context
	 * @param logFile the log file
	 */
	private void initializeWithConventions(LoggingInitializationContext initializationContext, LogFile logFile) {
		String config = getSelfInitializationConfig();
		if (config != null && logFile == null) {
			// self initialization has occurred, reinitialize in case of property changes
			reinitialize(initializationContext);
			return;
		}
		if (config == null) {
			config = getSpringInitializationConfig();
		}
		if (config != null) {
			loadConfiguration(initializationContext, config, logFile);
			return;
		}
		loadDefaults(initializationContext, logFile);
	}

	/**
	 * Return any self initialization config that has been applied. By default this method
	 * checks {@link #getStandardConfigLocations()} and assumes that any file that exists
	 * will have been applied.
	 * @return the self initialization config or {@code null}
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

	/**
	 * Finds the configuration file from the given locations.
	 * @param locations an array of locations to search for the configuration file
	 * @return the path of the configuration file if found, otherwise null
	 */
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
	 * @return the spring config locations
	 * @see #getSpringInitializationConfig()
	 */
	protected String[] getSpringConfigLocations() {
		String[] locations = getStandardConfigLocations();
		for (int i = 0; i < locations.length; i++) {
			String extension = StringUtils.getFilenameExtension(locations[i]);
			locations[i] = locations[i].substring(0, locations[i].length() - extension.length() - 1) + "-spring."
					+ extension;
		}
		return locations;
	}

	/**
	 * Load sensible defaults for the logging system.
	 * @param initializationContext the logging initialization context
	 * @param logFile the file to load or {@code null} if no log file is to be written
	 */
	protected abstract void loadDefaults(LoggingInitializationContext initializationContext, LogFile logFile);

	/**
	 * Load a specific configuration.
	 * @param initializationContext the logging initialization context
	 * @param location the location of the configuration to load (never {@code null})
	 * @param logFile the file to load or {@code null} if no log file is to be written
	 */
	protected abstract void loadConfiguration(LoggingInitializationContext initializationContext, String location,
			LogFile logFile);

	/**
	 * Reinitialize the logging system if required. Called when
	 * {@link #getSelfInitializationConfig()} is used and the log file hasn't changed. May
	 * be used to reload configuration (for example to pick up additional System
	 * properties).
	 * @param initializationContext the logging initialization context
	 */
	protected void reinitialize(LoggingInitializationContext initializationContext) {
	}

	/**
	 * Returns the ClassLoader used by this AbstractLoggingSystem.
	 * @return the ClassLoader used by this AbstractLoggingSystem
	 */
	protected final ClassLoader getClassLoader() {
		return this.classLoader;
	}

	/**
	 * Returns the path of the packaged configuration file with the given file name. The
	 * path is generated based on the package name of the current class. The file is
	 * assumed to be located in the same package as the class. The path is returned in the
	 * format "classpath:package/path/fileName".
	 * @param fileName the name of the configuration file
	 * @return the path of the packaged configuration file
	 */
	protected final String getPackagedConfigFile(String fileName) {
		String defaultPath = ClassUtils.getPackageName(getClass());
		defaultPath = defaultPath.replace('.', '/');
		defaultPath = defaultPath + "/" + fileName;
		defaultPath = "classpath:" + defaultPath;
		return defaultPath;
	}

	/**
	 * Applies system properties to the given environment and log file.
	 * @param environment the environment to apply system properties to
	 * @param logFile the log file to apply system properties to
	 */
	protected final void applySystemProperties(Environment environment, LogFile logFile) {
		new LoggingSystemProperties(environment, getDefaultValueResolver(environment), null).apply(logFile);
	}

	/**
	 * Return the default value resolver to use when resolving system properties.
	 * @param environment the environment
	 * @return the default value resolver
	 * @since 3.2.0
	 */
	protected Function<String, String> getDefaultValueResolver(Environment environment) {
		String defaultLogCorrelationPattern = getDefaultLogCorrelationPattern();
		return (name) -> {
			if (StringUtils.hasLength(defaultLogCorrelationPattern)
					&& LoggingSystemProperty.CORRELATION_PATTERN.getApplicationPropertyName().equals(name)
					&& environment.getProperty(LoggingSystem.EXPECT_CORRELATION_ID_PROPERTY, Boolean.class, false)) {
				return defaultLogCorrelationPattern;
			}
			return null;
		};
	}

	/**
	 * Return the default log correlation pattern or {@code null} if log correlation
	 * patterns are not supported.
	 * @return the default log correlation pattern
	 * @since 3.2.0
	 */
	protected String getDefaultLogCorrelationPattern() {
		return null;
	}

	/**
	 * Maintains a mapping between native levels and {@link LogLevel}.
	 *
	 * @param <T> the native level type
	 */
	protected static class LogLevels<T> {

		private final Map<LogLevel, T> systemToNative;

		private final Map<T, LogLevel> nativeToSystem;

		/**
		 * Constructs a new instance of the LogLevels class. Initializes the
		 * systemToNative EnumMap with LogLevel as the key type. Initializes the
		 * nativeToSystem HashMap.
		 */
		public LogLevels() {
			this.systemToNative = new EnumMap<>(LogLevel.class);
			this.nativeToSystem = new HashMap<>();
		}

		/**
		 * Maps a system log level to a native log level and vice versa.
		 * @param system the system log level to be mapped
		 * @param nativeLevel the native log level to be mapped
		 * @param <T> the type of the native log level
		 */
		public void map(LogLevel system, T nativeLevel) {
			this.systemToNative.putIfAbsent(system, nativeLevel);
			this.nativeToSystem.putIfAbsent(nativeLevel, system);
		}

		/**
		 * Converts a native log level to the system log level.
		 * @param level the native log level to be converted
		 * @return the corresponding system log level
		 */
		public LogLevel convertNativeToSystem(T level) {
			return this.nativeToSystem.get(level);
		}

		/**
		 * Converts a system log level to its corresponding native log level.
		 * @param level the system log level to be converted
		 * @return the native log level corresponding to the given system log level
		 */
		public T convertSystemToNative(LogLevel level) {
			return this.systemToNative.get(level);
		}

		/**
		 * Returns a set of supported log levels.
		 * @return a set of supported log levels
		 */
		public Set<LogLevel> getSupported() {
			return new LinkedHashSet<>(this.nativeToSystem.values());
		}

	}

}
