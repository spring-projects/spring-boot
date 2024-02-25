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

package org.springframework.boot.logging.log4j2;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.AbstractConfiguration;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.config.composite.CompositeConfiguration;
import org.apache.logging.log4j.core.filter.AbstractFilter;
import org.apache.logging.log4j.core.net.UrlConnectionFactory;
import org.apache.logging.log4j.core.net.ssl.SslConfiguration;
import org.apache.logging.log4j.core.net.ssl.SslConfigurationFactory;
import org.apache.logging.log4j.core.util.AuthorizationProvider;
import org.apache.logging.log4j.core.util.NameUtil;
import org.apache.logging.log4j.jul.Log4jBridgeHandler;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.util.PropertiesUtil;

import org.springframework.boot.context.properties.bind.BindResult;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.logging.AbstractLoggingSystem;
import org.springframework.boot.logging.LogFile;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.LoggerConfiguration;
import org.springframework.boot.logging.LoggerConfiguration.LevelConfiguration;
import org.springframework.boot.logging.LoggingInitializationContext;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.boot.logging.LoggingSystemFactory;
import org.springframework.core.Conventions;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;

/**
 * {@link LoggingSystem} for <a href="https://logging.apache.org/log4j/2.x/">Log4j 2</a>.
 *
 * @author Daniel Fullarton
 * @author Andy Wilkinson
 * @author Alexander Heusingfeld
 * @author Ben Hale
 * @author Ralph Goers
 * @since 1.2.0
 */
public class Log4J2LoggingSystem extends AbstractLoggingSystem {

	private static final String FILE_PROTOCOL = "file";

	private static final String LOG4J_BRIDGE_HANDLER = "org.apache.logging.log4j.jul.Log4jBridgeHandler";

	private static final String LOG4J_LOG_MANAGER = "org.apache.logging.log4j.jul.LogManager";

	static final String ENVIRONMENT_KEY = Conventions.getQualifiedAttributeName(Log4J2LoggingSystem.class,
			"environment");

	private static final LogLevels<Level> LEVELS = new LogLevels<>();

	static {
		LEVELS.map(LogLevel.TRACE, Level.TRACE);
		LEVELS.map(LogLevel.DEBUG, Level.DEBUG);
		LEVELS.map(LogLevel.INFO, Level.INFO);
		LEVELS.map(LogLevel.WARN, Level.WARN);
		LEVELS.map(LogLevel.ERROR, Level.ERROR);
		LEVELS.map(LogLevel.FATAL, Level.FATAL);
		LEVELS.map(LogLevel.OFF, Level.OFF);
	}

	private static final Filter FILTER = new AbstractFilter() {

		/**
		 * Filters the given log event.
		 * @param event the log event to be filtered
		 * @return the result of the filtering operation, which is Result.DENY in this
		 * case
		 */
		@Override
		public Result filter(LogEvent event) {
			return Result.DENY;
		}

		/**
		 * Filters log events based on the specified criteria.
		 * @param logger the logger associated with the log event
		 * @param level the level of the log event
		 * @param marker the marker associated with the log event
		 * @param msg the message of the log event
		 * @param t the throwable associated with the log event
		 * @return the result of the filtering process
		 */
		@Override
		public Result filter(Logger logger, Level level, Marker marker, Message msg, Throwable t) {
			return Result.DENY;
		}

		/**
		 * Filters log events based on the specified criteria.
		 * @param logger the logger associated with the log event
		 * @param level the level of the log event
		 * @param marker the marker associated with the log event
		 * @param msg the message of the log event
		 * @param t the throwable associated with the log event
		 * @return the result of the filtering process
		 */
		@Override
		public Result filter(Logger logger, Level level, Marker marker, Object msg, Throwable t) {
			return Result.DENY;
		}

		/**
		 * Filters log messages based on the specified criteria.
		 * @param logger the logger instance
		 * @param level the log level
		 * @param marker the log marker
		 * @param msg the log message
		 * @param params the log message parameters
		 * @return the result of the filter operation
		 */
		@Override
		public Result filter(Logger logger, Level level, Marker marker, String msg, Object... params) {
			return Result.DENY;
		}

	};

	/**
	 * Constructs a new Log4J2LoggingSystem with the specified class loader.
	 * @param classLoader the class loader to be used for loading classes and resources
	 */
	public Log4J2LoggingSystem(ClassLoader classLoader) {
		super(classLoader);
	}

	/**
	 * Returns an array of standard configuration file locations for Log4J2. The locations
	 * are determined based on the availability of certain classes and properties.
	 *
	 * The standard configuration file locations are as follows: -
	 * "log4j2-test.properties" - "log4j2-test.yaml" and "log4j2-test.yml" (if
	 * com.fasterxml.jackson.dataformat.yaml.YAMLParser is available) - "log4j2-test.json"
	 * and "log4j2-test.jsn" (if com.fasterxml.jackson.databind.ObjectMapper is available)
	 * - "log4j2-test.xml" - "log4j2.properties" - "log4j2.yaml" and "log4j2.yml" (if
	 * com.fasterxml.jackson.dataformat.yaml.YAMLParser is available) - "log4j2.json" and
	 * "log4j2.jsn" (if com.fasterxml.jackson.databind.ObjectMapper is available) -
	 * "log4j2.xml" - The location specified by the "log4j.configurationFile" system
	 * property
	 * @return an array of standard configuration file locations
	 */
	@Override
	protected String[] getStandardConfigLocations() {
		List<String> locations = new ArrayList<>();
		locations.add("log4j2-test.properties");
		if (isClassAvailable("com.fasterxml.jackson.dataformat.yaml.YAMLParser")) {
			Collections.addAll(locations, "log4j2-test.yaml", "log4j2-test.yml");
		}
		if (isClassAvailable("com.fasterxml.jackson.databind.ObjectMapper")) {
			Collections.addAll(locations, "log4j2-test.json", "log4j2-test.jsn");
		}
		locations.add("log4j2-test.xml");
		locations.add("log4j2.properties");
		if (isClassAvailable("com.fasterxml.jackson.dataformat.yaml.YAMLParser")) {
			Collections.addAll(locations, "log4j2.yaml", "log4j2.yml");
		}
		if (isClassAvailable("com.fasterxml.jackson.databind.ObjectMapper")) {
			Collections.addAll(locations, "log4j2.json", "log4j2.jsn");
		}
		locations.add("log4j2.xml");
		String propertyDefinedLocation = new PropertiesUtil(new Properties())
			.getStringProperty(ConfigurationFactory.CONFIGURATION_FILE_PROPERTY);
		if (propertyDefinedLocation != null) {
			locations.add(propertyDefinedLocation);
		}
		return StringUtils.toStringArray(locations);
	}

	/**
	 * Checks if a class is available.
	 * @param className the name of the class to check
	 * @return true if the class is available, false otherwise
	 */
	protected boolean isClassAvailable(String className) {
		return ClassUtils.isPresent(className, getClassLoader());
	}

	/**
	 * This method is called before the initialization of the Log4J2LoggingSystem. It
	 * checks if the logger context is already initialized and if not, it adds a filter to
	 * the logger configuration. If the JDK logging bridge handler cannot be configured,
	 * it calls the super method before initialization.
	 *
	 * @see LoggerContext
	 * @see Log4J2LoggingSystem#isAlreadyInitialized(LoggerContext)
	 * @see Log4J2LoggingSystem#configureJdkLoggingBridgeHandler()
	 * @see Configuration#addFilter(Filter)
	 */
	@Override
	public void beforeInitialize() {
		LoggerContext loggerContext = getLoggerContext();
		if (isAlreadyInitialized(loggerContext)) {
			return;
		}
		if (!configureJdkLoggingBridgeHandler()) {
			super.beforeInitialize();
		}
		loggerContext.getConfiguration().addFilter(FILTER);
	}

	/**
	 * Configures the JDK logging bridge handler.
	 *
	 * This method checks if the Java Util Logging (JUL) is using a single console handler
	 * at most, if Log4j LogManager is not installed, and if the Log4j bridge handler is
	 * available. If all conditions are met, the default root handler is removed and the
	 * Log4j bridge handler is installed.
	 * @return true if the JDK logging bridge handler is successfully configured, false
	 * otherwise.
	 */
	private boolean configureJdkLoggingBridgeHandler() {
		try {
			if (isJulUsingASingleConsoleHandlerAtMost() && !isLog4jLogManagerInstalled()
					&& isLog4jBridgeHandlerAvailable()) {
				removeDefaultRootHandler();
				Log4jBridgeHandler.install(false, null, true);
				return true;
			}
		}
		catch (Throwable ex) {
			// Ignore. No java.util.logging bridge is installed.
		}
		return false;
	}

	/**
	 * Checks if the JUL (Java Util Logging) is using a single ConsoleHandler at most.
	 * @return true if JUL is using a single ConsoleHandler at most, false otherwise
	 */
	private boolean isJulUsingASingleConsoleHandlerAtMost() {
		java.util.logging.Logger rootLogger = java.util.logging.LogManager.getLogManager().getLogger("");
		Handler[] handlers = rootLogger.getHandlers();
		return handlers.length == 0 || (handlers.length == 1 && handlers[0] instanceof ConsoleHandler);
	}

	/**
	 * Checks if Log4j LogManager is installed.
	 * @return true if Log4j LogManager is installed, false otherwise.
	 */
	private boolean isLog4jLogManagerInstalled() {
		final String logManagerClassName = java.util.logging.LogManager.getLogManager().getClass().getName();
		return LOG4J_LOG_MANAGER.equals(logManagerClassName);
	}

	/**
	 * Checks if the Log4j Bridge Handler is available.
	 * @return {@code true} if the Log4j Bridge Handler is available, {@code false}
	 * otherwise.
	 */
	private boolean isLog4jBridgeHandlerAvailable() {
		return ClassUtils.isPresent(LOG4J_BRIDGE_HANDLER, getClassLoader());
	}

	/**
	 * Removes the Log4jBridgeHandler from the root logger. This method closes the handler
	 * and removes it from the root logger if it is an instance of Log4jBridgeHandler.
	 *
	 * @see Log4jBridgeHandler
	 */
	private void removeLog4jBridgeHandler() {
		removeDefaultRootHandler();
		java.util.logging.Logger rootLogger = java.util.logging.LogManager.getLogManager().getLogger("");
		for (final Handler handler : rootLogger.getHandlers()) {
			if (handler instanceof Log4jBridgeHandler) {
				handler.close();
				rootLogger.removeHandler(handler);
			}
		}
	}

	/**
	 * Removes the default root handler from the Log4J2LoggingSystem class. This method
	 * checks if the root logger has only one handler, which is an instance of
	 * ConsoleHandler, and removes it if found.
	 * @throws Throwable if an error occurs while removing the default root handler
	 *
	 * @since 1.0
	 */
	private void removeDefaultRootHandler() {
		try {
			java.util.logging.Logger rootLogger = java.util.logging.LogManager.getLogManager().getLogger("");
			Handler[] handlers = rootLogger.getHandlers();
			if (handlers.length == 1 && handlers[0] instanceof ConsoleHandler) {
				rootLogger.removeHandler(handlers[0]);
			}
		}
		catch (Throwable ex) {
			// Ignore and continue
		}
	}

	/**
	 * Initializes the logging system with the given initialization context, config
	 * location, and log file.
	 * @param initializationContext the logging initialization context
	 * @param configLocation the location of the logging configuration file
	 * @param logFile the log file to be used
	 */
	@Override
	public void initialize(LoggingInitializationContext initializationContext, String configLocation, LogFile logFile) {
		LoggerContext loggerContext = getLoggerContext();
		if (isAlreadyInitialized(loggerContext)) {
			return;
		}
		Environment environment = initializationContext.getEnvironment();
		if (environment != null) {
			getLoggerContext().putObjectIfAbsent(ENVIRONMENT_KEY, environment);
			PropertiesUtil.getProperties().addPropertySource(new SpringEnvironmentPropertySource(environment));
		}
		loggerContext.getConfiguration().removeFilter(FILTER);
		super.initialize(initializationContext, configLocation, logFile);
		markAsInitialized(loggerContext);
	}

	/**
	 * Loads the default configuration for the logging system.
	 * @param initializationContext the initialization context for the logging system
	 * @param logFile the log file to be used, or null if not applicable
	 */
	@Override
	protected void loadDefaults(LoggingInitializationContext initializationContext, LogFile logFile) {
		String location = getPackagedConfigFile((logFile != null) ? "log4j2-file.xml" : "log4j2.xml");
		load(initializationContext, location, logFile);
	}

	/**
	 * Loads the configuration for the logging system.
	 * @param initializationContext the initialization context for the logging system
	 * @param location the location of the configuration file
	 * @param logFile the log file to be used
	 */
	@Override
	protected void loadConfiguration(LoggingInitializationContext initializationContext, String location,
			LogFile logFile) {
		load(initializationContext, location, logFile);
	}

	/**
	 * Loads the configuration for the Log4J2LoggingSystem.
	 * @param initializationContext the LoggingInitializationContext object
	 * @param location the location of the configuration file
	 * @param logFile the LogFile object
	 */
	private void load(LoggingInitializationContext initializationContext, String location, LogFile logFile) {
		List<String> overrides = getOverrides(initializationContext);
		if (initializationContext != null) {
			applySystemProperties(initializationContext.getEnvironment(), logFile);
		}
		loadConfiguration(location, logFile, overrides);
	}

	/**
	 * Retrieves the list of log4j2 configuration overrides from the provided
	 * initialization context.
	 * @param initializationContext the logging initialization context
	 * @return the list of log4j2 configuration overrides
	 */
	private List<String> getOverrides(LoggingInitializationContext initializationContext) {
		BindResult<List<String>> overrides = Binder.get(initializationContext.getEnvironment())
			.bind("logging.log4j2.config.override", Bindable.listOf(String.class));
		return overrides.orElse(Collections.emptyList());
	}

	/**
	 * Load the configuration from the given {@code location}, creating a composite using
	 * the configuration from the given {@code overrides}.
	 * @param location the location
	 * @param logFile log file configuration
	 * @param overrides the overriding locations
	 * @since 2.6.0
	 */
	protected void loadConfiguration(String location, LogFile logFile, List<String> overrides) {
		Assert.notNull(location, "Location must not be null");
		try {
			List<Configuration> configurations = new ArrayList<>();
			LoggerContext context = getLoggerContext();
			configurations.add(load(location, context));
			for (String override : overrides) {
				configurations.add(load(override, context));
			}
			Configuration configuration = (configurations.size() > 1) ? createComposite(configurations)
					: configurations.iterator().next();
			context.start(configuration);
		}
		catch (Exception ex) {
			throw new IllegalStateException("Could not initialize Log4J2 logging from " + location, ex);
		}
	}

	/**
	 * Loads the configuration from the specified location.
	 * @param location the location of the configuration file
	 * @param context the logger context
	 * @return the loaded configuration
	 * @throws IOException if an I/O error occurs while loading the configuration
	 */
	private Configuration load(String location, LoggerContext context) throws IOException {
		URL url = ResourceUtils.getURL(location);
		ConfigurationSource source = getConfigurationSource(url);
		return ConfigurationFactory.getInstance().getConfiguration(context, source);
	}

	/**
	 * Retrieves the configuration source for the given URL.
	 * @param url The URL of the configuration source.
	 * @return The configuration source.
	 * @throws IOException If an I/O error occurs while retrieving the configuration
	 * source.
	 */
	private ConfigurationSource getConfigurationSource(URL url) throws IOException {
		if (FILE_PROTOCOL.equals(url.getProtocol())) {
			return new ConfigurationSource(url.openStream(), ResourceUtils.getFile(url));
		}
		AuthorizationProvider authorizationProvider = ConfigurationFactory
			.authorizationProvider(PropertiesUtil.getProperties());
		SslConfiguration sslConfiguration = url.getProtocol().equals("https")
				? SslConfigurationFactory.getSslConfiguration() : null;
		URLConnection connection = UrlConnectionFactory.createConnection(url, 0, sslConfiguration,
				authorizationProvider);
		return new ConfigurationSource(connection.getInputStream(), url, connection.getLastModified());
	}

	/**
	 * Creates a composite configuration by combining multiple configurations.
	 * @param configurations the list of configurations to be combined
	 * @return a composite configuration object
	 */
	private CompositeConfiguration createComposite(List<Configuration> configurations) {
		return new CompositeConfiguration(configurations.stream().map(AbstractConfiguration.class::cast).toList());
	}

	/**
	 * Reinitializes the logging system with the given initialization context.
	 * @param initializationContext the logging initialization context
	 */
	@Override
	protected void reinitialize(LoggingInitializationContext initializationContext) {
		List<String> overrides = getOverrides(initializationContext);
		if (!CollectionUtils.isEmpty(overrides)) {
			reinitializeWithOverrides(overrides);
		}
		else {
			LoggerContext context = getLoggerContext();
			context.reconfigure();
		}
	}

	/**
	 * Reinitializes the Log4J2 logging system with the provided overrides.
	 * @param overrides a list of strings representing the paths to the overriding
	 * configurations
	 * @throws RuntimeException if failed to load the overriding configuration from any of
	 * the provided paths
	 */
	private void reinitializeWithOverrides(List<String> overrides) {
		LoggerContext context = getLoggerContext();
		Configuration base = context.getConfiguration();
		List<AbstractConfiguration> configurations = new ArrayList<>();
		configurations.add((AbstractConfiguration) base);
		for (String override : overrides) {
			try {
				configurations.add((AbstractConfiguration) load(override, context));
			}
			catch (IOException ex) {
				throw new RuntimeException("Failed to load overriding configuration from '" + override + "'", ex);
			}
		}
		CompositeConfiguration composite = new CompositeConfiguration(configurations);
		context.reconfigure(composite);
	}

	/**
	 * Returns a set of supported log levels.
	 * @return a set of supported log levels
	 */
	@Override
	public Set<LogLevel> getSupportedLogLevels() {
		return LEVELS.getSupported();
	}

	/**
	 * Sets the log level for a specific logger.
	 * @param loggerName the name of the logger
	 * @param logLevel the log level to be set
	 */
	@Override
	public void setLogLevel(String loggerName, LogLevel logLevel) {
		setLogLevel(loggerName, LEVELS.convertSystemToNative(logLevel));
	}

	/**
	 * Sets the log level for a specific logger.
	 * @param loggerName the name of the logger
	 * @param level the log level to be set
	 */
	private void setLogLevel(String loggerName, Level level) {
		LoggerConfig logger = getLogger(loggerName);
		if (level == null) {
			clearLogLevel(loggerName, logger);
		}
		else {
			setLogLevel(loggerName, logger, level);
		}
		getLoggerContext().updateLoggers();
	}

	/**
	 * Clears the log level for the specified logger.
	 * @param loggerName the name of the logger
	 * @param logger the logger configuration
	 */
	private void clearLogLevel(String loggerName, LoggerConfig logger) {
		if (logger instanceof LevelSetLoggerConfig) {
			getLoggerContext().getConfiguration().removeLogger(loggerName);
		}
		else {
			logger.setLevel(null);
		}
	}

	/**
	 * Sets the log level for a specific logger.
	 * @param loggerName the name of the logger
	 * @param logger the logger configuration
	 * @param level the log level to set
	 */
	private void setLogLevel(String loggerName, LoggerConfig logger, Level level) {
		if (logger == null) {
			getLoggerContext().getConfiguration()
				.addLogger(loggerName, new LevelSetLoggerConfig(loggerName, level, true));
		}
		else {
			logger.setLevel(level);
		}
	}

	/**
	 * Retrieves a list of all logger configurations.
	 * @return A list of LoggerConfiguration objects representing the logger
	 * configurations.
	 */
	@Override
	public List<LoggerConfiguration> getLoggerConfigurations() {
		List<LoggerConfiguration> result = new ArrayList<>();
		getAllLoggers().forEach((name, loggerConfig) -> result.add(convertLoggerConfig(name, loggerConfig)));
		result.sort(CONFIGURATION_COMPARATOR);
		return result;
	}

	/**
	 * Retrieves the configuration for a specific logger.
	 * @param loggerName the name of the logger
	 * @return the configuration for the specified logger, or null if the logger does not
	 * exist
	 */
	@Override
	public LoggerConfiguration getLoggerConfiguration(String loggerName) {
		LoggerConfig loggerConfig = getAllLoggers().get(loggerName);
		return (loggerConfig != null) ? convertLoggerConfig(loggerName, loggerConfig) : null;
	}

	/**
	 * Retrieves all the loggers and their configurations.
	 * @return a map containing the loggers and their configurations
	 */
	private Map<String, LoggerConfig> getAllLoggers() {
		Map<String, LoggerConfig> loggers = new LinkedHashMap<>();
		for (Logger logger : getLoggerContext().getLoggers()) {
			addLogger(loggers, logger.getName());
		}
		getLoggerContext().getConfiguration().getLoggers().keySet().forEach((name) -> addLogger(loggers, name));
		return loggers;
	}

	/**
	 * Adds a logger to the given map of loggers.
	 * @param loggers the map of loggers to add the logger to
	 * @param name the name of the logger to add
	 */
	private void addLogger(Map<String, LoggerConfig> loggers, String name) {
		Configuration configuration = getLoggerContext().getConfiguration();
		while (name != null) {
			loggers.computeIfAbsent(name, configuration::getLoggerConfig);
			name = getSubName(name);
		}
	}

	/**
	 * Returns the sub name of the given name.
	 * @param name the name to get the sub name from
	 * @return the sub name of the given name, or null if the name is empty or null
	 */
	private String getSubName(String name) {
		if (!StringUtils.hasLength(name)) {
			return null;
		}
		int nested = name.lastIndexOf('$');
		return (nested != -1) ? name.substring(0, nested) : NameUtil.getSubName(name);
	}

	/**
	 * Converts a LoggerConfig object to a LoggerConfiguration object.
	 * @param name The name of the logger.
	 * @param loggerConfig The LoggerConfig object to be converted.
	 * @return The converted LoggerConfiguration object.
	 */
	private LoggerConfiguration convertLoggerConfig(String name, LoggerConfig loggerConfig) {
		if (loggerConfig == null) {
			return null;
		}
		LevelConfiguration effectiveLevelConfiguration = getLevelConfiguration(loggerConfig.getLevel());
		if (!StringUtils.hasLength(name) || LogManager.ROOT_LOGGER_NAME.equals(name)) {
			name = ROOT_LOGGER_NAME;
		}
		boolean isAssigned = loggerConfig.getName().equals(name);
		LevelConfiguration assignedLevelConfiguration = (!isAssigned) ? null : effectiveLevelConfiguration;
		return new LoggerConfiguration(name, assignedLevelConfiguration, effectiveLevelConfiguration);
	}

	/**
	 * Retrieves the level configuration for a given level.
	 * @param level the level for which the configuration is to be retrieved
	 * @return the level configuration for the specified level
	 */
	private LevelConfiguration getLevelConfiguration(Level level) {
		LogLevel logLevel = LEVELS.convertNativeToSystem(level);
		return (logLevel != null) ? LevelConfiguration.of(logLevel) : LevelConfiguration.ofCustom(level.name());
	}

	/**
	 * Returns a Runnable object that can be used as a shutdown handler for the
	 * Log4J2LoggingSystem. The returned Runnable object stops the logger context.
	 * @return a Runnable object that stops the logger context
	 */
	@Override
	public Runnable getShutdownHandler() {
		return () -> getLoggerContext().stop();
	}

	/**
	 * Cleans up the Log4J2LoggingSystem by removing the Log4j Bridge Handler, marking the
	 * logger context as uninitialized, and removing the filter from the logger context
	 * configuration. If the Log4j Bridge Handler is available, it will be removed. This
	 * method overrides the cleanUp() method from the superclass.
	 */
	@Override
	public void cleanUp() {
		if (isLog4jBridgeHandlerAvailable()) {
			removeLog4jBridgeHandler();
		}
		super.cleanUp();
		LoggerContext loggerContext = getLoggerContext();
		markAsUninitialized(loggerContext);
		loggerContext.getConfiguration().removeFilter(FILTER);
	}

	/**
	 * Retrieves the LoggerConfig object for the specified logger name. If the name is
	 * empty or equal to the ROOT_LOGGER_NAME constant, the method returns the
	 * LoggerConfig object for the root logger.
	 * @param name the name of the logger
	 * @return the LoggerConfig object for the specified logger name
	 */
	private LoggerConfig getLogger(String name) {
		boolean isRootLogger = !StringUtils.hasLength(name) || ROOT_LOGGER_NAME.equals(name);
		return findLogger(isRootLogger ? LogManager.ROOT_LOGGER_NAME : name);
	}

	/**
	 * Finds the LoggerConfig with the specified name.
	 * @param name the name of the LoggerConfig to find
	 * @return the LoggerConfig with the specified name, or null if not found
	 */
	private LoggerConfig findLogger(String name) {
		Configuration configuration = getLoggerContext().getConfiguration();
		if (configuration instanceof AbstractConfiguration abstractConfiguration) {
			return abstractConfiguration.getLogger(name);
		}
		return configuration.getLoggers().get(name);
	}

	/**
	 * Retrieves the LoggerContext instance associated with the Log4J2LoggingSystem class.
	 * @return The LoggerContext instance associated with the Log4J2LoggingSystem class.
	 */
	private LoggerContext getLoggerContext() {
		return (LoggerContext) LogManager.getContext(false);
	}

	/**
	 * Checks if the logger context is already initialized.
	 * @param loggerContext the logger context to check
	 * @return {@code true} if the logger context is already initialized, {@code false}
	 * otherwise
	 */
	private boolean isAlreadyInitialized(LoggerContext loggerContext) {
		return LoggingSystem.class.getName().equals(loggerContext.getExternalContext());
	}

	/**
	 * Marks the logger context as initialized by setting the external context to the
	 * class name of LoggingSystem.
	 * @param loggerContext the logger context to be marked as initialized
	 */
	private void markAsInitialized(LoggerContext loggerContext) {
		loggerContext.setExternalContext(LoggingSystem.class.getName());
	}

	/**
	 * Marks the logger context as uninitialized by setting the external context to null.
	 * @param loggerContext the logger context to mark as uninitialized
	 */
	private void markAsUninitialized(LoggerContext loggerContext) {
		loggerContext.setExternalContext(null);
	}

	/**
	 * Returns the default log correlation pattern.
	 * @return the default log correlation pattern ("%correlationId")
	 */
	@Override
	protected String getDefaultLogCorrelationPattern() {
		return "%correlationId";
	}

	/**
	 * Get the Spring {@link Environment} attached to the given {@link LoggerContext} or
	 * {@code null} if no environment is available.
	 * @param loggerContext the logger context
	 * @return the Spring {@link Environment} or {@code null}
	 * @since 3.0.0
	 */
	public static Environment getEnvironment(LoggerContext loggerContext) {
		return (Environment) ((loggerContext != null) ? loggerContext.getObject(ENVIRONMENT_KEY) : null);
	}

	/**
	 * {@link LoggingSystemFactory} that returns {@link Log4J2LoggingSystem} if possible.
	 */
	@Order(Ordered.LOWEST_PRECEDENCE)
	public static class Factory implements LoggingSystemFactory {

		private static final boolean PRESENT = ClassUtils
			.isPresent("org.apache.logging.log4j.core.impl.Log4jContextFactory", Factory.class.getClassLoader());

		/**
		 * Returns the logging system based on the specified class loader.
		 * @param classLoader the class loader to be used
		 * @return the logging system if it is present, otherwise null
		 */
		@Override
		public LoggingSystem getLoggingSystem(ClassLoader classLoader) {
			if (PRESENT) {
				return new Log4J2LoggingSystem(classLoader);
			}
			return null;
		}

	}

	/**
	 * {@link LoggerConfig} used when the user has set a specific {@link Level}.
	 */
	private static class LevelSetLoggerConfig extends LoggerConfig {

		/**
		 * Constructs a new LevelSetLoggerConfig with the specified name, level, and
		 * additive flag.
		 * @param name the name of the logger configuration
		 * @param level the logging level for the logger configuration
		 * @param additive the flag indicating whether the logger configuration is
		 * additive
		 */
		LevelSetLoggerConfig(String name, Level level, boolean additive) {
			super(name, level, additive);
		}

	}

}
