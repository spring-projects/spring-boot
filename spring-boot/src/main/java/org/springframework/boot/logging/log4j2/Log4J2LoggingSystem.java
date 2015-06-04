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

package org.springframework.boot.logging.log4j2;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.filter.AbstractFilter;
import org.apache.logging.log4j.message.Message;
import org.springframework.boot.logging.LogFile;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.boot.logging.Slf4JLoggingSystem;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ResourceUtils;

/**
 * {@link LoggingSystem} for <a href="http://logging.apache.org/log4j/2.x/">Log4j 2</a>.
 *
 * @author Daniel Fullarton
 * @author Andy Wilkinson
 * @author Alexander Heusingfeld
 * @since 1.2.0
 */
public class Log4J2LoggingSystem extends Slf4JLoggingSystem {

	private static final Map<LogLevel, Level> LEVELS;
	static {
		Map<LogLevel, Level> levels = new HashMap<LogLevel, Level>();
		levels.put(LogLevel.TRACE, Level.TRACE);
		levels.put(LogLevel.DEBUG, Level.DEBUG);
		levels.put(LogLevel.INFO, Level.INFO);
		levels.put(LogLevel.WARN, Level.WARN);
		levels.put(LogLevel.ERROR, Level.ERROR);
		levels.put(LogLevel.FATAL, Level.ERROR);
		levels.put(LogLevel.OFF, Level.OFF);
		LEVELS = Collections.unmodifiableMap(levels);
	}

	private static final Filter FILTER = new AbstractFilter() {

		@Override
		public Result filter(LogEvent event) {
			return Result.DENY;
		}

		@Override
		public Result filter(Logger logger, Level level, Marker marker, Message msg,
				Throwable t) {
			return Result.DENY;
		}

		@Override
		public Result filter(Logger logger, Level level, Marker marker, Object msg,
				Throwable t) {
			return Result.DENY;
		}

		@Override
		public Result filter(Logger logger, Level level, Marker marker, String msg,
				Object... params) {
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
		List<String> supportedConfigLocations = new ArrayList<String>();
		if (isClassAvailable("com.fasterxml.jackson.dataformat.yaml.YAMLParser")) {
			Collections.addAll(supportedConfigLocations, "log4j2.yaml", "log4j2.yml");
		}
		if (isClassAvailable("com.fasterxml.jackson.databind.ObjectMapper")) {
			Collections.addAll(supportedConfigLocations, "log4j2.json", "log4j2.jsn");
		}
		supportedConfigLocations.add("log4j2.xml");
		return supportedConfigLocations.toArray(new String[supportedConfigLocations
				.size()]);
	}

	protected boolean isClassAvailable(String className) {
		return ClassUtils.isPresent(className, getClassLoader());
	}

	@Override
	public void beforeInitialize() {
		super.beforeInitialize();
		getLoggerConfig(null).addFilter(FILTER);
	}

	@Override
	public void initialize(String configLocation, LogFile logFile) {
		getLoggerConfig(null).removeFilter(FILTER);
		super.initialize(configLocation, logFile);
	}

	@Override
	protected void loadDefaults(LogFile logFile) {
		if (logFile != null) {
			loadConfiguration(getPackagedConfigFile("log4j2-file.xml"), logFile);
		}
		else {
			loadConfiguration(getPackagedConfigFile("log4j2.xml"), logFile);
		}
	}

	@Override
	protected void loadConfiguration(String location, LogFile logFile) {
		Assert.notNull(location, "Location must not be null");
		if (logFile != null) {
			logFile.applyToSystemProperties();
		}
		try {
			LoggerContext ctx = getLoggerContext();
			URL url = ResourceUtils.getURL(location);
			ConfigurationSource source = getConfigurationSource(url);
			ctx.start(ConfigurationFactory.getInstance().getConfiguration(source));
		}
		catch (Exception ex) {
			throw new IllegalStateException("Could not initialize Log4J2 logging from "
					+ location, ex);
		}
	}

	private ConfigurationSource getConfigurationSource(URL url) throws IOException {
		InputStream stream = url.openStream();
		if (ResourceUtils.isFileURL(url)) {
			return new ConfigurationSource(stream, ResourceUtils.getFile(url));
		}
		return new ConfigurationSource(stream, url);
	}

	@Override
	protected void reinitialize() {
		getLoggerContext().reconfigure();
	}

	@Override
	public void setLogLevel(String loggerName, LogLevel level) {
		getLoggerConfig(loggerName).setLevel(LEVELS.get(level));
		getLoggerContext().updateLoggers();
	}

	private LoggerConfig getLoggerConfig(String loggerName) {
		LoggerConfig loggerConfig = getLoggerContext().getConfiguration()
				.getLoggerConfig(loggerName == null ? "" : loggerName);
		return loggerConfig;
	}

	private LoggerContext getLoggerContext() {
		return (LoggerContext) LogManager.getContext(false);
	}

}
