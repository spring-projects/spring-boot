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

package org.springframework.boot.logging.java;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.springframework.boot.logging.AbstractLoggingSystem;
import org.springframework.boot.logging.LogFile;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.LoggerConfiguration;
import org.springframework.boot.logging.LoggingInitializationContext;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.boot.logging.LoggingSystemFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;

/**
 * {@link LoggingSystem} for {@link Logger java.util.logging}.
 *
 * @author Phillip Webb
 * @author Dave Syer
 * @author Andy Wilkinson
 * @author Ben Hale
 * @since 1.0.0
 */
public class JavaLoggingSystem extends AbstractLoggingSystem {

	private static final LogLevels<Level> LEVELS = new LogLevels<>();

	static {
		LEVELS.map(LogLevel.TRACE, Level.FINEST);
		LEVELS.map(LogLevel.DEBUG, Level.FINE);
		LEVELS.map(LogLevel.INFO, Level.INFO);
		LEVELS.map(LogLevel.WARN, Level.WARNING);
		LEVELS.map(LogLevel.ERROR, Level.SEVERE);
		LEVELS.map(LogLevel.FATAL, Level.SEVERE);
		LEVELS.map(LogLevel.OFF, Level.OFF);
	}

	private final Set<Logger> configuredLoggers = Collections.synchronizedSet(new HashSet<>());

	/**
     * Constructs a new JavaLoggingSystem with the specified class loader.
     *
     * @param classLoader the class loader to be used for loading classes and resources
     */
    public JavaLoggingSystem(ClassLoader classLoader) {
		super(classLoader);
	}

	/**
     * Returns an array of standard configuration locations.
     * 
     * @return an array of standard configuration locations
     */
    @Override
	protected String[] getStandardConfigLocations() {
		return new String[] { "logging.properties" };
	}

	/**
     * This method is called before the initialization of the JavaLoggingSystem.
     * It overrides the superclass method and sets the logging level of the root logger to SEVERE.
     */
    @Override
	public void beforeInitialize() {
		super.beforeInitialize();
		Logger.getLogger("").setLevel(Level.SEVERE);
	}

	/**
     * Loads the default logging configuration based on the provided initialization context and log file.
     * If a log file is provided, the configuration is loaded from the "logging-file.properties" file.
     * If no log file is provided, the configuration is loaded from the "logging.properties" file.
     *
     * @param initializationContext The initialization context for logging.
     * @param logFile The log file to load the configuration from. Can be null if no log file is provided.
     */
    @Override
	protected void loadDefaults(LoggingInitializationContext initializationContext, LogFile logFile) {
		if (logFile != null) {
			loadConfiguration(getPackagedConfigFile("logging-file.properties"), logFile);
		}
		else {
			loadConfiguration(getPackagedConfigFile("logging.properties"), null);
		}
	}

	/**
     * Loads the configuration for the logging system.
     * 
     * @param initializationContext the initialization context for the logging system
     * @param location the location of the configuration file
     * @param logFile the log file to be used
     */
    @Override
	protected void loadConfiguration(LoggingInitializationContext initializationContext, String location,
			LogFile logFile) {
		loadConfiguration(location, logFile);
	}

	/**
     * Loads the configuration for the Java logging system.
     * 
     * @param location the location of the configuration file
     * @param logFile the log file to be used in the configuration
     * @throws IllegalStateException if the Java logging system could not be initialized
     * @throws IllegalArgumentException if the location is null
     */
    protected void loadConfiguration(String location, LogFile logFile) {
		Assert.notNull(location, "Location must not be null");
		try {
			String configuration = FileCopyUtils
				.copyToString(new InputStreamReader(ResourceUtils.getURL(location).openStream()));
			if (logFile != null) {
				configuration = configuration.replace("${LOG_FILE}", StringUtils.cleanPath(logFile.toString()));
			}
			LogManager.getLogManager().readConfiguration(new ByteArrayInputStream(configuration.getBytes()));
		}
		catch (Exception ex) {
			throw new IllegalStateException("Could not initialize Java logging from " + location, ex);
		}
	}

	/**
     * Returns a set of supported log levels.
     *
     * @return a set of supported log levels
     */
    @Override
	public Set<LogLevel> getSupportedLogLevels() {
		return LEVELS.getSupported();
	}

	/**
     * Sets the log level for a specific logger.
     * 
     * @param loggerName the name of the logger
     * @param level the log level to be set
     */
    @Override
	public void setLogLevel(String loggerName, LogLevel level) {
		if (loggerName == null || ROOT_LOGGER_NAME.equals(loggerName)) {
			loggerName = "";
		}
		Logger logger = Logger.getLogger(loggerName);
		if (logger != null) {
			this.configuredLoggers.add(logger);
			logger.setLevel(LEVELS.convertSystemToNative(level));
		}
	}

	/**
     * Retrieves a list of all logger configurations.
     * 
     * @return A list of LoggerConfiguration objects representing the configurations of all loggers.
     */
    @Override
	public List<LoggerConfiguration> getLoggerConfigurations() {
		List<LoggerConfiguration> result = new ArrayList<>();
		Enumeration<String> names = LogManager.getLogManager().getLoggerNames();
		while (names.hasMoreElements()) {
			result.add(getLoggerConfiguration(names.nextElement()));
		}
		result.sort(CONFIGURATION_COMPARATOR);
		return Collections.unmodifiableList(result);
	}

	/**
     * Retrieves the configuration of a logger with the specified name.
     * 
     * @param loggerName the name of the logger
     * @return the configuration of the logger, or null if the logger does not exist
     */
    @Override
	public LoggerConfiguration getLoggerConfiguration(String loggerName) {
		Logger logger = Logger.getLogger(loggerName);
		if (logger == null) {
			return null;
		}
		LogLevel level = LEVELS.convertNativeToSystem(logger.getLevel());
		LogLevel effectiveLevel = LEVELS.convertNativeToSystem(getEffectiveLevel(logger));
		String name = (StringUtils.hasLength(logger.getName()) ? logger.getName() : ROOT_LOGGER_NAME);
		return new LoggerConfiguration(name, level, effectiveLevel);
	}

	/**
     * Retrieves the effective logging level for the specified root logger.
     * 
     * @param root the root logger to retrieve the effective level from
     * @return the effective logging level for the root logger
     */
    private Level getEffectiveLevel(Logger root) {
		Logger logger = root;
		while (logger.getLevel() == null) {
			logger = logger.getParent();
		}
		return logger.getLevel();
	}

	/**
     * Returns a Runnable object that can be used as a shutdown handler.
     * This handler resets the logging configuration by calling the reset() method of the LogManager class.
     *
     * @return a Runnable object that resets the logging configuration
     */
    @Override
	public Runnable getShutdownHandler() {
		return () -> LogManager.getLogManager().reset();
	}

	/**
     * Cleans up the configured loggers.
     */
    @Override
	public void cleanUp() {
		this.configuredLoggers.clear();
	}

	/**
	 * {@link LoggingSystemFactory} that returns {@link JavaLoggingSystem} if possible.
	 */
	@Order(Ordered.LOWEST_PRECEDENCE)
	public static class Factory implements LoggingSystemFactory {

		private static final boolean PRESENT = ClassUtils.isPresent("java.util.logging.LogManager",
				Factory.class.getClassLoader());

		/**
         * Returns the logging system based on the provided class loader.
         * 
         * @param classLoader the class loader to be used for loading the logging system
         * @return the logging system if it is present, otherwise null
         */
        @Override
		public LoggingSystem getLoggingSystem(ClassLoader classLoader) {
			if (PRESENT) {
				return new JavaLoggingSystem(classLoader);
			}
			return null;
		}

	}

}
