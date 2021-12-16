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

package org.springframework.boot.logging.log4j2;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
import org.apache.logging.log4j.core.util.NameUtil;
import org.apache.logging.log4j.message.Message;

import org.springframework.boot.context.properties.bind.BindResult;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.logging.LogFile;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.LoggerConfiguration;
import org.springframework.boot.logging.LoggingInitializationContext;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.boot.logging.LoggingSystemFactory;
import org.springframework.boot.logging.Slf4JLoggingSystem;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
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
 * @since 1.2.0
 */
public class Log4J2LoggingSystem extends Slf4JLoggingSystem {

	private static final String FILE_PROTOCOL = "file";

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

		@Override
		public Result filter(LogEvent event) {
			return Result.DENY;
		}

		@Override
		public Result filter(Logger logger, Level level, Marker marker, Message msg, Throwable t) {
			return Result.DENY;
		}

		@Override
		public Result filter(Logger logger, Level level, Marker marker, Object msg, Throwable t) {
			return Result.DENY;
		}

		@Override
		public Result filter(Logger logger, Level level, Marker marker, String msg, Object... params) {
			return Result.DENY;
		}

	};

	public Log4J2LoggingSystem(ClassLoader classLoader) {
		super(classLoader);
	}

	@Override
	protected String[] getStandardConfigLocations() {
		return getCurrentlySupportedConfigLocations();
	}

	private String[] getCurrentlySupportedConfigLocations() {
		List<String> supportedConfigLocations = new ArrayList<>();
		addTestFiles(supportedConfigLocations);
		supportedConfigLocations.add("log4j2.properties");
		if (isClassAvailable("com.fasterxml.jackson.dataformat.yaml.YAMLParser")) {
			Collections.addAll(supportedConfigLocations, "log4j2.yaml", "log4j2.yml");
		}
		if (isClassAvailable("com.fasterxml.jackson.databind.ObjectMapper")) {
			Collections.addAll(supportedConfigLocations, "log4j2.json", "log4j2.jsn");
		}
		supportedConfigLocations.add("log4j2.xml");
		return StringUtils.toStringArray(supportedConfigLocations);
	}

	private void addTestFiles(List<String> supportedConfigLocations) {
		supportedConfigLocations.add("log4j2-test.properties");
		if (isClassAvailable("com.fasterxml.jackson.dataformat.yaml.YAMLParser")) {
			Collections.addAll(supportedConfigLocations, "log4j2-test.yaml", "log4j2-test.yml");
		}
		if (isClassAvailable("com.fasterxml.jackson.databind.ObjectMapper")) {
			Collections.addAll(supportedConfigLocations, "log4j2-test.json", "log4j2-test.jsn");
		}
		supportedConfigLocations.add("log4j2-test.xml");
	}

	protected boolean isClassAvailable(String className) {
		return ClassUtils.isPresent(className, getClassLoader());
	}

	@Override
	public void beforeInitialize() {
		LoggerContext loggerContext = getLoggerContext();
		if (isAlreadyInitialized(loggerContext)) {
			return;
		}
		super.beforeInitialize();
		loggerContext.getConfiguration().addFilter(FILTER);
	}

	@Override
	public void initialize(LoggingInitializationContext initializationContext, String configLocation, LogFile logFile) {
		LoggerContext loggerContext = getLoggerContext();
		if (isAlreadyInitialized(loggerContext)) {
			return;
		}
		loggerContext.getConfiguration().removeFilter(FILTER);
		super.initialize(initializationContext, configLocation, logFile);
		markAsInitialized(loggerContext);
	}

	@Override
	protected void loadDefaults(LoggingInitializationContext initializationContext, LogFile logFile) {
		if (logFile != null) {
			loadConfiguration(getPackagedConfigFile("log4j2-file.xml"), logFile, getOverrides(initializationContext));
		}
		else {
			loadConfiguration(getPackagedConfigFile("log4j2.xml"), logFile, getOverrides(initializationContext));
		}
	}

	private List<String> getOverrides(LoggingInitializationContext initializationContext) {
		BindResult<List<String>> overrides = Binder.get(initializationContext.getEnvironment())
				.bind("logging.log4j2.config.override", Bindable.listOf(String.class));
		return overrides.orElse(Collections.emptyList());
	}

	@Override
	protected void loadConfiguration(LoggingInitializationContext initializationContext, String location,
			LogFile logFile) {
		super.loadConfiguration(initializationContext, location, logFile);
		loadConfiguration(location, logFile, getOverrides(initializationContext));
	}

	/**
	 * Load the configuration from the given {@code location}.
	 * @param location the location
	 * @param logFile log file configuration
	 * @deprecated since 2.6.0 for removal in 2.8.0 in favor of
	 * {@link #loadConfiguration(String, LogFile, List)}
	 */
	@Deprecated
	protected void loadConfiguration(String location, LogFile logFile) {
		this.loadConfiguration(location, logFile, Collections.emptyList());
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

	private Configuration load(String location, LoggerContext context) throws IOException {
		URL url = ResourceUtils.getURL(location);
		ConfigurationSource source = getConfigurationSource(url);
		return ConfigurationFactory.getInstance().getConfiguration(context, source);
	}

	private ConfigurationSource getConfigurationSource(URL url) throws IOException {
		InputStream stream = url.openStream();
		if (FILE_PROTOCOL.equals(url.getProtocol())) {
			return new ConfigurationSource(stream, ResourceUtils.getFile(url));
		}
		return new ConfigurationSource(stream, url);
	}

	private CompositeConfiguration createComposite(List<Configuration> configurations) {
		return new CompositeConfiguration(
				configurations.stream().map(AbstractConfiguration.class::cast).collect(Collectors.toList()));
	}

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

	@Override
	public Set<LogLevel> getSupportedLogLevels() {
		return LEVELS.getSupported();
	}

	@Override
	public void setLogLevel(String loggerName, LogLevel logLevel) {
		setLogLevel(loggerName, LEVELS.convertSystemToNative(logLevel));
	}

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

	private void clearLogLevel(String loggerName, LoggerConfig logger) {
		if (logger instanceof LevelSetLoggerConfig) {
			getLoggerContext().getConfiguration().removeLogger(loggerName);
		}
		else {
			logger.setLevel(null);
		}
	}

	private void setLogLevel(String loggerName, LoggerConfig logger, Level level) {
		if (logger == null) {
			getLoggerContext().getConfiguration().addLogger(loggerName,
					new LevelSetLoggerConfig(loggerName, level, true));
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
	public LoggerConfiguration getLoggerConfiguration(String loggerName) {
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

	private String getSubName(String name) {
		if (!StringUtils.hasLength(name)) {
			return null;
		}
		int nested = name.lastIndexOf('$');
		return (nested != -1) ? name.substring(0, nested) : NameUtil.getSubName(name);
	}

	private LoggerConfiguration convertLoggerConfig(String name, LoggerConfig loggerConfig) {
		if (loggerConfig == null) {
			return null;
		}
		LogLevel level = LEVELS.convertNativeToSystem(loggerConfig.getLevel());
		if (!StringUtils.hasLength(name) || LogManager.ROOT_LOGGER_NAME.equals(name)) {
			name = ROOT_LOGGER_NAME;
		}
		boolean isLoggerConfigured = loggerConfig.getName().equals(name);
		LogLevel configuredLevel = (isLoggerConfigured) ? level : null;
		return new LoggerConfiguration(name, configuredLevel, level);
	}

	@Override
	public Runnable getShutdownHandler() {
		return () -> getLoggerContext().stop();
	}

	@Override
	public void cleanUp() {
		super.cleanUp();
		LoggerContext loggerContext = getLoggerContext();
		markAsUninitialized(loggerContext);
		loggerContext.getConfiguration().removeFilter(FILTER);
	}

	private LoggerConfig getLogger(String name) {
		boolean isRootLogger = !StringUtils.hasLength(name) || ROOT_LOGGER_NAME.equals(name);
		return findLogger(isRootLogger ? LogManager.ROOT_LOGGER_NAME : name);
	}

	private LoggerConfig findLogger(String name) {
		Configuration configuration = getLoggerContext().getConfiguration();
		if (configuration instanceof AbstractConfiguration) {
			return ((AbstractConfiguration) configuration).getLogger(name);
		}
		return configuration.getLoggers().get(name);
	}

	private LoggerContext getLoggerContext() {
		return (LoggerContext) LogManager.getContext(false);
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

	/**
	 * {@link LoggingSystemFactory} that returns {@link Log4J2LoggingSystem} if possible.
	 */
	@Order(Ordered.LOWEST_PRECEDENCE)
	public static class Factory implements LoggingSystemFactory {

		private static final boolean PRESENT = ClassUtils
				.isPresent("org.apache.logging.log4j.core.impl.Log4jContextFactory", Factory.class.getClassLoader());

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

		LevelSetLoggerConfig(String name, Level level, boolean additive) {
			super(name, level, additive);
		}

	}

}
