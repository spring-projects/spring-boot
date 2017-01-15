/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.logging.log4j2;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.DefaultConfiguration;
import org.apache.logging.log4j.core.config.LoggerConfig;

import org.springframework.boot.logging.LogFile;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.LoggerConfiguration;
import org.springframework.boot.logging.LoggingInitializationContext;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.boot.logging.Slf4JLoggingSystem;
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
 * @author Matt Sicker
 * @since 1.2.0
 */
public class Log4J2LoggingSystem extends Slf4JLoggingSystem {

	private static final Object EXTERNAL_CONTEXT = LoggingSystem.class.getName();

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

	private final AtomicBoolean initialized = new AtomicBoolean(false);

	public Log4J2LoggingSystem(ClassLoader classLoader) {
		super(classLoader);
	}

	@Override
	protected String[] getStandardConfigLocations() {
		return getCurrentlySupportedConfigLocations();
	}

	private String[] getCurrentlySupportedConfigLocations() {
		List<String> supportedConfigLocations = new ArrayList<>();
		if (isClassAvailable("com.fasterxml.jackson.dataformat.yaml.YAMLParser")) {
			Collections.addAll(supportedConfigLocations, "log4j2.yaml", "log4j2.yml");
		}
		if (isClassAvailable("com.fasterxml.jackson.databind.ObjectMapper")) {
			Collections.addAll(supportedConfigLocations, "log4j2.json", "log4j2.jsn");
		}
		supportedConfigLocations.add("log4j2.properties");
		supportedConfigLocations.add("log4j2.xml");
		return supportedConfigLocations
				.toArray(new String[supportedConfigLocations.size()]);
	}

	protected boolean isClassAvailable(String className) {
		return ClassUtils.isPresent(className, getClassLoader());
	}

	@Override
	public void initialize(LoggingInitializationContext initializationContext,
			String configLocation, LogFile logFile) {
		if (this.initialized.compareAndSet(false, true)) {
			super.initialize(initializationContext, configLocation, logFile);
		}
	}

	@Override
	protected void loadDefaults(LoggingInitializationContext initializationContext,
			LogFile logFile) {
		String fileName = "log4j2" + (logFile == null ? "" : "-file") + ".xml";
		loadConfiguration(getPackagedConfigFile(fileName), logFile);
	}

	@Override
	protected void loadConfiguration(LoggingInitializationContext initializationContext,
			String location, LogFile logFile) {
		super.loadConfiguration(initializationContext, location, logFile);
		loadConfiguration(location, logFile);
	}

	protected void loadConfiguration(String location, LogFile logFile) {
		Assert.notNull(location, "Location must not be null");
		URI uri;
		try {
			uri = ResourceUtils.toURI(location);
		}
		catch (Exception ex) {
			throw new IllegalStateException(
					"Could not initialize Log4J2 logging from " + location, ex);
		}
		LoggerContext ctx = getLoggerContext(uri);
		if (ctx == null || ctx.getConfiguration() instanceof DefaultConfiguration) {
			// a DefaultConfiguration is returned in case there's an error with the
			// provided config file, but in our case, we specifically wanted it to work
			// and a simple error log message isn't enough
			throw new IllegalStateException(
					"Could not initialize Log4J2 logging from " + location);
		}
	}

	private LoggerContext getLoggerContext(URI uri) {
		return (LoggerContext) LogManager
				.getContext(getClassLoader(), false, EXTERNAL_CONTEXT, uri);
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
		Configurator.setLevel(loggerName, level);
	}

	@Override
	public List<LoggerConfiguration> getLoggerConfigurations() {
		return getLoggerContext().getConfiguration().getLoggers().values().stream()
				.map(this::convertLoggerConfiguration)
				.sorted(CONFIGURATION_COMPARATOR)
				.collect(Collectors.toList());
	}

	@Override
	public LoggerConfiguration getLoggerConfiguration(String loggerName) {
		return convertLoggerConfiguration(getLoggerConfig(loggerName));
	}

	private LoggerConfiguration convertLoggerConfiguration(LoggerConfig loggerConfig) {
		if (loggerConfig == null) {
			return null;
		}
		LogLevel level = LEVELS.convertNativeToSystem(loggerConfig.getLevel());
		String name = loggerConfig.getName();
		if (!StringUtils.hasLength(name) || LogManager.ROOT_LOGGER_NAME.equals(name)) {
			name = ROOT_LOGGER_NAME;
		}
		return new LoggerConfiguration(name, level, level);
	}

	@Override
	public void cleanUp() {
		super.cleanUp();
		LogManager.shutdown();
		this.initialized.set(false);
	}

	private LoggerConfig getLoggerConfig(String name) {
		if (!StringUtils.hasLength(name) || ROOT_LOGGER_NAME.equals(name)) {
			name = LogManager.ROOT_LOGGER_NAME;
		}
		return getLoggerContext().getConfiguration().getLoggers().get(name);
	}

	private LoggerContext getLoggerContext() {
		return (LoggerContext) LogManager
				.getContext(getClassLoader(), false, EXTERNAL_CONTEXT);
	}

}
