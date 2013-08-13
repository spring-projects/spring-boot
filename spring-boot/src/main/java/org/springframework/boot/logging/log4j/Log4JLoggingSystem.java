/*
 * Copyright 2012-2013 the original author or authors.
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

package org.springframework.boot.logging.log4j;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.boot.logging.AbstractLoggingSystem;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.util.Assert;
import org.springframework.util.Log4jConfigurer;
import org.springframework.util.StringUtils;

/**
 * {@link LoggingSystem} for for <a href="http://logging.apache.org/log4j">log4j</a>.
 * 
 * @author Phillip Webb
 * @author Dave Syer
 */
public class Log4JLoggingSystem extends AbstractLoggingSystem {

	private static final Map<LogLevel, Level> LEVELS;
	static {
		Map<LogLevel, Level> levels = new HashMap<LogLevel, Level>();
		levels.put(LogLevel.TRACE, Level.TRACE);
		levels.put(LogLevel.DEBUG, Level.DEBUG);
		levels.put(LogLevel.INFO, Level.INFO);
		levels.put(LogLevel.WARN, Level.WARN);
		levels.put(LogLevel.ERROR, Level.ERROR);
		levels.put(LogLevel.FATAL, Level.ERROR);
		LEVELS = Collections.unmodifiableMap(levels);
	}

	public Log4JLoggingSystem(ClassLoader classLoader) {
		super(classLoader, "log4j.xml", "log4j.properties");
	}

	@Override
	public void initialize(String configLocation) {
		Assert.notNull(configLocation, "ConfigLocation must not be null");
		try {
			Log4jConfigurer.initLogging(configLocation);
		}
		catch (Exception ex) {
			throw new IllegalStateException("Could not initialize logging from "
					+ configLocation, ex);
		}
	}

	@Override
	public void setLogLevel(String loggerName, LogLevel level) {
		Logger logger = (StringUtils.hasLength(loggerName) ? LogManager
				.getLogger(loggerName) : LogManager.getRootLogger());
		logger.setLevel(LEVELS.get(level));
	}

}
