/*
 * Copyright 2012-2020 the original author or authors.
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
import org.apache.logging.log4j.core.filter.AbstractFilter;
import org.apache.logging.log4j.core.util.NameUtil;
import org.apache.logging.log4j.message.Message;

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
			loadConfiguration(getPackagedConfigFile("log4j2-file.xml"), logFile);
		}
		else {
			loadConfiguration(getPackagedConfigFile("log4j2.xml"), logFile);
		}
	}

	@Override
	protected void loadConfiguration(LoggingInitializationContext initializationContext, String location,
			LogFile logFile) {
		super.loadConfiguration(initializationContext, location, logFile);
		loadConfiguration(location, logFile);
	}

	protected void loadConfiguration(String location, LogFile logFile) {
		Assert.notNull(location, "Location must not be null");
		try {
			LoggerContext ctx = getLoggerContext();
			URL url = ResourceUtils.getURL(location);
			ConfigurationSource source = getConfigurationSource(url);
			ctx.start(ConfigurationFactory.getInstance().getConfiguration(ctx, source));
		}
		catch (Exception ex) {
			throw new IllegalStateException("Could not initialize Log4J2 logging from " + location, ex);
		}
	}

	private ConfigurationSource getConfigurationSource(URL url) throws IOException {
		InputStream stream = url.openStream();
		if (FILE_PROTOCOL.equals(url.getProtocol())) {
			return new ConfigurationSource(stream, ResourceUtils.getFile(url));
		}
		return new ConfigurationSource(stream, url);
	}

	@Override
	protected void reinitialize(LoggingInitializationContext initializationContext) {
		getLoggerContext().reconfigure();
	}

	@Override
	public Set<LogLevel> getSupportedLogLevels() {
		return LEVELS.getSupported();
	}

	@Override
	public void setLogLevel(String loggerName, LogLevel logLevel) {
		Level level = LEVELS.convertSystemToNative(logLevel);
		LoggerConfig logger = getLogger(loggerName);
		if (logger == null) {
			logger = new LoggerConfig(loggerName, level, true);
			getLoggerContext().getConfiguration().addLogger(loggerName, logger);
		}
		else {
			logger.setLevel(level);
		}
		getLoggerContext().updateLoggers();
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
		if (StringUtils.isEmpty(name)) {
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
		return new ShutdownHandler();
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

	private final class ShutdownHandler implements Runnable {

		@Override
		public void run() {
			getLoggerContext().stop();
		}

	}

	/**
	 * {@link LoggingSystemFactory} that returns {@link Log4J2LoggingSystem} if possible.
	 */
	@Order(Ordered.LOWEST_PRECEDENCE)
	public static class Factory implements LoggingSystemFactory {

		@Override
		public LoggingSystem getLoggingSystem(ClassLoader classLoader) {
			if (ClassUtils.isPresent("org.apache.logging.log4j.core.impl.Log4jContextFactory", classLoader)) {
				return new Log4J2LoggingSystem(classLoader);
			}
			return null;
		}

	}

}
