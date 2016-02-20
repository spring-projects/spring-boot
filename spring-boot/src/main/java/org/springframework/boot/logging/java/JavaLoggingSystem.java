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

package org.springframework.boot.logging.java;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.springframework.boot.logging.AbstractLoggingSystem;
import org.springframework.boot.logging.LogFile;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.LoggingInitializationContext;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;

/**
 * {@link LoggingSystem} for {@link Logger java.util.logging}.
 *
 * @author Phillip Webb
 * @author Dave Syer
 * @author Andy Wilkinson
 */
public class JavaLoggingSystem extends AbstractLoggingSystem {

	private static final Map<LogLevel, Level> LEVELS;

	static {
		Map<LogLevel, Level> levels = new HashMap<LogLevel, Level>();
		levels.put(LogLevel.TRACE, Level.FINEST);
		levels.put(LogLevel.DEBUG, Level.FINE);
		levels.put(LogLevel.INFO, Level.INFO);
		levels.put(LogLevel.WARN, Level.WARNING);
		levels.put(LogLevel.ERROR, Level.SEVERE);
		levels.put(LogLevel.FATAL, Level.SEVERE);
		levels.put(LogLevel.OFF, Level.OFF);
		LEVELS = Collections.unmodifiableMap(levels);
	}

	public JavaLoggingSystem(ClassLoader classLoader) {
		super(classLoader);
	}

	@Override
	protected String[] getStandardConfigLocations() {
		return new String[] { "logging.properties" };
	}

	@Override
	public void beforeInitialize() {
		super.beforeInitialize();
		Logger.getLogger("").setLevel(Level.SEVERE);
	}

	@Override
	protected void loadDefaults(LoggingInitializationContext initializationContext,
			LogFile logFile) {
		if (logFile != null) {
			loadConfiguration(getPackagedConfigFile("logging-file.properties"), logFile);
		}
		else {
			loadConfiguration(getPackagedConfigFile("logging.properties"), logFile);
		}
	}

	@Override
	protected void loadConfiguration(LoggingInitializationContext initializationContext,
			String location, LogFile logFile) {
		loadConfiguration(location, logFile);
	}

	protected void loadConfiguration(String location, LogFile logFile) {
		Assert.notNull(location, "Location must not be null");
		try {
			String configuration = FileCopyUtils.copyToString(
					new InputStreamReader(ResourceUtils.getURL(location).openStream()));
			if (logFile != null) {
				configuration = configuration.replace("${LOG_FILE}",
						StringUtils.cleanPath(logFile.toString()));
			}
			LogManager.getLogManager().readConfiguration(
					new ByteArrayInputStream(configuration.getBytes()));
		}
		catch (Exception ex) {
			throw new IllegalStateException(
					"Could not initialize Java logging from " + location, ex);
		}
	}

	@Override
	public void setLogLevel(String loggerName, LogLevel level) {
		Assert.notNull(level, "Level must not be null");
		String name = (StringUtils.hasText(loggerName) ? loggerName : "");
		Logger logger = Logger.getLogger(name);
		logger.setLevel(LEVELS.get(level));
	}

	@Override
	public Runnable getShutdownHandler() {
		return new ShutdownHandler();
	}

	private final class ShutdownHandler implements Runnable {

		@Override
		public void run() {
			LogManager.getLogManager().reset();
		}

	}

}
