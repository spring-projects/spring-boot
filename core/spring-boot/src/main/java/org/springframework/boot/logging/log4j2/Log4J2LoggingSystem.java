/*
 * Copyright 2012-present the original author or authors.
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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.AbstractConfiguration;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationException;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.config.composite.CompositeConfiguration;
import org.apache.logging.log4j.core.filter.DenyAllFilter;
import org.apache.logging.log4j.core.util.NameUtil;
import org.apache.logging.log4j.jul.Log4jBridgeHandler;
import org.apache.logging.log4j.status.StatusConsoleListener;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.PropertiesUtil;
import org.jspecify.annotations.Nullable;

import org.springframework.boot.context.properties.bind.BindResult;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.io.ApplicationResourceLoader;
import org.springframework.boot.logging.AbstractLoggingSystem;
import org.springframework.boot.logging.LogFile;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.LoggerConfiguration;
import org.springframework.boot.logging.LoggerConfiguration.LevelConfiguration;
import org.springframework.boot.logging.LoggingInitializationContext;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.boot.logging.LoggingSystemFactory;
import org.springframework.core.Conventions;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * {@link LoggingSystem} for <a href="https://logging.apache.org/log4j/2.x/">Log4j 2</a>.
 *
 * @author Daniel Fullarton
 * @author Andy Wilkinson
 * @author Alexander Heusingfeld
 * @author Ben Hale
 * @author Ralph Goers
 * @author Piotr P. Karwasz
 * @since 1.2.0
 */
public class Log4J2LoggingSystem extends AbstractLoggingSystem {

	private static final String OPTIONAL_PREFIX = "optional:";

	/**
	 * JUL handler that routes messages to the Log4j API (optional dependency).
	 */
	static final String LOG4J_BRIDGE_HANDLER = "org.apache.logging.log4j.jul.Log4jBridgeHandler";

	/**
	 * JUL LogManager that routes messages to the Log4j API as the backend.
	 */
	static final String LOG4J_LOG_MANAGER = "org.apache.logging.log4j.jul.LogManager";

	static final String ENVIRONMENT_KEY = Conventions.getQualifiedAttributeName(Log4J2LoggingSystem.class,
			"environment");

	static final String STATUS_LISTENER_KEY = Conventions.getQualifiedAttributeName(Log4J2LoggingSystem.class,
			"statusListener");

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

	private static final Filter FILTER = DenyAllFilter.newBuilder().build();

	private static final SpringEnvironmentPropertySource propertySource = new SpringEnvironmentPropertySource();

	private static final org.apache.logging.log4j.Logger statusLogger = StatusLogger.getLogger();

	private final LoggerContext loggerContext;

	/**
	 * Create a new {@link Log4J2LoggingSystem} instance.
	 * @param classLoader the class loader to use.
	 * @param loggerContext the {@link LoggerContext} to use.
	 */
	Log4J2LoggingSystem(ClassLoader classLoader, LoggerContext loggerContext) {
		super(classLoader);
		this.loggerContext = loggerContext;
	}

	@Override
	protected String[] getStandardConfigLocations() {
		// With Log4J2 we use the ConfigurationFactory
		throw new IllegalStateException("Standard config locations cannot be used with Log4J2");
	}

	@Override
	protected @Nullable String getSelfInitializationConfig() {
		return getConfigLocation(getLoggerContext().getConfiguration());
	}

	@Override
	protected @Nullable String getSpringInitializationConfig() {
		ConfigurationFactory configurationFactory = ConfigurationFactory.getInstance();
		try {
			Configuration springConfiguration = configurationFactory.getConfiguration(getLoggerContext(), "-spring",
					null, getClassLoader());
			String configLocation = getConfigLocation(springConfiguration);
			return (configLocation != null && configLocation.contains("-spring")) ? configLocation : null;
		}
		catch (ConfigurationException ex) {
			statusLogger.warn("Could not load Spring-specific Log4j Core configuration", ex);
			return null;
		}
	}

	/**
	 * Return the configuration location. The result may be:
	 * <ul>
	 * <li>{@code null}: if DefaultConfiguration is used (no explicit config loaded)</li>
	 * <li>A file path: if provided explicitly by the user</li>
	 * <li>A URI: if loaded from the classpath default or a custom location</li>
	 * </ul>
	 * @param configuration the source configuration
	 * @return the config location or {@code null}
	 */
	private @Nullable String getConfigLocation(Configuration configuration) {
		return configuration.getConfigurationSource().getLocation();
	}

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

	private boolean isJulUsingASingleConsoleHandlerAtMost() {
		java.util.logging.Logger rootLogger = java.util.logging.LogManager.getLogManager().getLogger("");
		Handler[] handlers = rootLogger.getHandlers();
		return handlers.length == 0 || (handlers.length == 1 && handlers[0] instanceof ConsoleHandler);
	}

	private boolean isLog4jLogManagerInstalled() {
		final String logManagerClassName = java.util.logging.LogManager.getLogManager().getClass().getName();
		return LOG4J_LOG_MANAGER.equals(logManagerClassName);
	}

	private boolean isLog4jBridgeHandlerAvailable() {
		return ClassUtils.isPresent(LOG4J_BRIDGE_HANDLER, getClassLoader());
	}

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

	@Override
	public void initialize(LoggingInitializationContext initializationContext, @Nullable String configLocation,
			@Nullable LogFile logFile) {
		LoggerContext loggerContext = getLoggerContext();
		if (isAlreadyInitialized(loggerContext)) {
			return;
		}
		StatusConsoleListener listener = new StatusConsoleListener(Level.WARN);
		StatusLogger.getLogger().registerListener(listener);
		loggerContext.putObject(STATUS_LISTENER_KEY, listener);
		Environment environment = initializationContext.getEnvironment();
		if (environment != null) {
			loggerContext.putObject(ENVIRONMENT_KEY, environment);
			Log4J2LoggingSystem.propertySource.setEnvironment(environment);
			PropertiesUtil.getProperties().addPropertySource(Log4J2LoggingSystem.propertySource);
		}
		loggerContext.getConfiguration().removeFilter(FILTER);
		super.initialize(initializationContext, configLocation, logFile);
		markAsInitialized(loggerContext);
	}

	@Override
	protected void loadDefaults(LoggingInitializationContext initializationContext, @Nullable LogFile logFile) {
		String location = getPackagedConfigFile((logFile != null) ? "log4j2-file.xml" : "log4j2.xml");
		load(initializationContext, location, logFile);
	}

	@Override
	protected void loadConfiguration(LoggingInitializationContext initializationContext, String location,
			@Nullable LogFile logFile) {
		load(initializationContext, location, logFile);
	}

	private void load(LoggingInitializationContext initializationContext, String location, @Nullable LogFile logFile) {
		List<String> overrides = getOverrides(initializationContext);
		Environment environment = initializationContext.getEnvironment();
		Assert.state(environment != null, "'environment' must not be null");
		applySystemProperties(environment, logFile);
		reconfigure(location, overrides);
	}

	private List<String> getOverrides(LoggingInitializationContext initializationContext) {
		Environment environment = initializationContext.getEnvironment();
		Assert.state(environment != null, "'environment' must not be null");
		BindResult<List<String>> overrides = Binder.get(environment)
			.bind("logging.log4j2.config.override", Bindable.listOf(String.class));
		return overrides.orElse(Collections.emptyList());
	}

	private void reconfigure(String location, List<String> overrides) {
		Assert.notNull(location, "'location' must not be null");
		try {
			List<Configuration> configurations = new ArrayList<>();
			ResourceLoader resourceLoader = ApplicationResourceLoader.get(getClassLoader());
			configurations.add(load(resourceLoader, location));
			for (String override : overrides) {
				Configuration overrideConfiguration = loadOverride(resourceLoader, override);
				if (overrideConfiguration != null) {
					configurations.add(overrideConfiguration);
				}
			}
			this.loggerContext.reconfigure(mergeConfigurations(configurations));
		}
		catch (Exception ex) {
			throw new IllegalStateException("Could not initialize Log4J2 logging from %s%s".formatted(location,
					(overrides.isEmpty() ? "" : " with overrides " + overrides)), ex);
		}
	}

	private Configuration load(ResourceLoader resourceLoader, String location) throws IOException {
		ConfigurationFactory configurationFactory = ConfigurationFactory.getInstance();
		Resource resource = resourceLoader.getResource(location);
		Configuration configuration = configurationFactory.getConfiguration(getLoggerContext(), null, resource.getURI(),
				getClassLoader());
		// The error handling in Log4j Core 2.25.x is not consistent, some loading and
		// parsing errors result in a null configuration, others in an exception.
		if (configuration == null) {
			throw new ConfigurationException("Could not load Log4j Core configuration from " + location);
		}
		return configuration;
	}

	private @Nullable Configuration loadOverride(ResourceLoader resourceLoader, String location) throws IOException {
		if (location.startsWith(OPTIONAL_PREFIX)) {
			String actualLocation = location.substring(OPTIONAL_PREFIX.length());
			Resource resource = resourceLoader.getResource(actualLocation);
			try {
				return (resource.exists()) ? load(resourceLoader, actualLocation) : null;
			}
			catch (FileNotFoundException ex) {
				return null;
			}
		}
		return load(resourceLoader, location);
	}

	private Configuration mergeConfigurations(List<Configuration> configurations) {
		if (configurations.size() == 1) {
			return configurations.iterator().next();
		}
		return new CompositeConfiguration(configurations.stream().map(AbstractConfiguration.class::cast).toList());
	}

	@Override
	protected void reinitialize(LoggingInitializationContext initializationContext) {
		String currentLocation = getSelfInitializationConfig();
		Assert.notNull(currentLocation, "'currentLocation' must not be null");
		load(initializationContext, currentLocation, null);
	}

	@Override
	public Set<LogLevel> getSupportedLogLevels() {
		return LEVELS.getSupported();
	}

	@Override
	public void setLogLevel(@Nullable String loggerName, @Nullable LogLevel logLevel) {
		setLogLevel(loggerName, LEVELS.convertSystemToNative(logLevel));
	}

	private void setLogLevel(@Nullable String loggerName, @Nullable Level level) {
		LoggerConfig logger = getLogger(loggerName);
		if (level == null) {
			clearLogLevel(loggerName, logger);
		}
		else {
			setLogLevel(loggerName, logger, level);
		}
		getLoggerContext().updateLoggers();
	}

	private void clearLogLevel(@Nullable String loggerName, @Nullable LoggerConfig logger) {
		if (logger == null) {
			return;
		}
		if (logger instanceof LevelSetLoggerConfig) {
			getLoggerContext().getConfiguration().removeLogger(loggerName);
		}
		else {
			logger.setLevel(null);
		}
	}

	private void setLogLevel(@Nullable String loggerName, @Nullable LoggerConfig logger, Level level) {
		if (logger == null) {
			getLoggerContext().getConfiguration()
				.addLogger(loggerName, new LevelSetLoggerConfig(loggerName, level, true));
		}
		else {
			logger.setLevel(level);
		}
	}

	@Override
	public List<LoggerConfiguration> getLoggerConfigurations() {
		List<LoggerConfiguration> result = new ArrayList<>();
		getAllLoggers().forEach((name, loggerConfig) -> result.add(convertLoggerConfig(name, loggerConfig)));
		result.sort(CONFIGURATION_COMPARATOR);
		return result;
	}

	@Override
	public @Nullable LoggerConfiguration getLoggerConfiguration(String loggerName) {
		LoggerConfig loggerConfig = getAllLoggers().get(loggerName);
		return (loggerConfig != null) ? convertLoggerConfig(loggerName, loggerConfig) : null;
	}

	private Map<String, LoggerConfig> getAllLoggers() {
		Map<String, LoggerConfig> loggers = new LinkedHashMap<>();
		for (Logger logger : getLoggerContext().getLoggers()) {
			addLogger(loggers, logger.getName());
		}
		getLoggerContext().getConfiguration().getLoggers().keySet().forEach((name) -> addLogger(loggers, name));
		return loggers;
	}

	private void addLogger(Map<String, LoggerConfig> loggers, String name) {
		Configuration configuration = getLoggerContext().getConfiguration();
		while (name != null) {
			loggers.computeIfAbsent(name, configuration::getLoggerConfig);
			name = getSubName(name);
		}
	}

	private @Nullable String getSubName(String name) {
		if (!StringUtils.hasLength(name)) {
			return null;
		}
		int nested = name.lastIndexOf('$');
		return (nested != -1) ? name.substring(0, nested) : NameUtil.getSubName(name);
	}

	private @Nullable LoggerConfiguration convertLoggerConfig(String name, @Nullable LoggerConfig loggerConfig) {
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

	private LevelConfiguration getLevelConfiguration(Level level) {
		LogLevel logLevel = LEVELS.convertNativeToSystem(level);
		return (logLevel != null) ? LevelConfiguration.of(logLevel) : LevelConfiguration.ofCustom(level.name());
	}

	@Override
	public Runnable getShutdownHandler() {
		return () -> getLoggerContext().stop();
	}

	@Override
	public void cleanUp() {
		if (isLog4jBridgeHandlerAvailable()) {
			removeLog4jBridgeHandler();
		}
		super.cleanUp();
		LoggerContext loggerContext = getLoggerContext();
		markAsUninitialized(loggerContext);
		StatusConsoleListener listener = (StatusConsoleListener) loggerContext.getObject(STATUS_LISTENER_KEY);
		if (listener != null) {
			StatusLogger.getLogger().removeListener(listener);
			loggerContext.removeObject(STATUS_LISTENER_KEY);
		}
		loggerContext.getConfiguration().removeFilter(FILTER);
		Log4J2LoggingSystem.propertySource.setEnvironment(null);
		loggerContext.removeObject(ENVIRONMENT_KEY);
	}

	private @Nullable LoggerConfig getLogger(@Nullable String name) {
		if (!StringUtils.hasLength(name) || ROOT_LOGGER_NAME.equals(name)) {
			return findLogger(LogManager.ROOT_LOGGER_NAME);
		}
		return findLogger(name);
	}

	private @Nullable LoggerConfig findLogger(String name) {
		Configuration configuration = getLoggerContext().getConfiguration();
		if (configuration instanceof AbstractConfiguration abstractConfiguration) {
			return abstractConfiguration.getLogger(name);
		}
		return configuration.getLoggers().get(name);
	}

	LoggerContext getLoggerContext() {
		return this.loggerContext;
	}

	private boolean isAlreadyInitialized(LoggerContext loggerContext) {
		return LoggingSystem.class.getName().equals(loggerContext.getExternalContext());
	}

	private void markAsInitialized(LoggerContext loggerContext) {
		loggerContext.setExternalContext(LoggingSystem.class.getName());
	}

	private void markAsUninitialized(LoggerContext loggerContext) {
		loggerContext.setExternalContext(null);
	}

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
	public static @Nullable Environment getEnvironment(@Nullable LoggerContext loggerContext) {
		return (Environment) ((loggerContext != null) ? loggerContext.getObject(ENVIRONMENT_KEY) : null);
	}

	/**
	 * {@link LoggingSystemFactory} that returns {@link Log4J2LoggingSystem} if possible.
	 */
	@Order(0)
	public static class Factory implements LoggingSystemFactory {

		static final String LOG4J_CORE_CONTEXT_FACTORY = "org.apache.logging.log4j.core.impl.Log4jContextFactory";

		private static final boolean PRESENT = ClassUtils.isPresent(LOG4J_CORE_CONTEXT_FACTORY,
				Factory.class.getClassLoader());

		@Override
		public @Nullable LoggingSystem getLoggingSystem(ClassLoader classLoader) {
			if (PRESENT) {
				org.apache.logging.log4j.spi.LoggerContext spiLoggerContext = LogManager.getContext(classLoader, false);
				Assert.state(spiLoggerContext instanceof LoggerContext, "");
				if (spiLoggerContext instanceof LoggerContext coreLoggerContext) {
					return new Log4J2LoggingSystem(classLoader, coreLoggerContext);
				}
			}
			return null;
		}

	}

	/**
	 * {@link LoggerConfig} used when the user has set a specific {@link Level}.
	 */
	private static class LevelSetLoggerConfig extends LoggerConfig {

		LevelSetLoggerConfig(@Nullable String name, Level level, boolean additive) {
			super(name, level, additive);
		}

	}

}
