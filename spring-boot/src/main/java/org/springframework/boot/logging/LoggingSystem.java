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

package org.springframework.boot.logging;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * Common abstraction over logging systems.
 *
 * @author Phillip Webb
 * @author Dave Syer
 * @author Andy Wilkinson
 */
public abstract class LoggingSystem {

	/**
	 * A System property that can be used to indicate the {@link LoggingSystem} to use.
	 */
	public static final String SYSTEM_PROPERTY = LoggingSystem.class.getName();

	private static final Map<String, String> SYSTEMS;

	static {
		Map<String, String> systems = new LinkedHashMap<String, String>();
		systems.put("ch.qos.logback.core.Appender",
				"org.springframework.boot.logging.logback.LogbackLoggingSystem");
		systems.put("org.apache.logging.log4j.LogManager",
				"org.springframework.boot.logging.log4j2.Log4J2LoggingSystem");
		systems.put("org.apache.log4j.PropertyConfigurator",
				"org.springframework.boot.logging.log4j.Log4JLoggingSystem");
		systems.put("java.util.logging.LogManager",
				"org.springframework.boot.logging.java.JavaLoggingSystem");
		SYSTEMS = Collections.unmodifiableMap(systems);
	}

	/**
	 * Reset the logging system to be limit output. This method may be called before
	 * {@link #initialize(LoggingInitializationContext, String, LogFile)} to reduce
	 * logging noise until the system has been fully initialized.
	 */
	public abstract void beforeInitialize();

	/**
	 * Fully initialize the logging system.
	 * @param initializationContext the logging initialization context
	 * @param configLocation a log configuration location or {@code null} if default
	 * initialization is required
	 * @param logFile the log output file that should be written or {@code null} for
	 * console only output
	 */
	public void initialize(LoggingInitializationContext initializationContext,
			String configLocation, LogFile logFile) {
		initialize(configLocation, logFile);
	}

	/**
	 * Fully initialize the logging system.
	 * @param configLocation a log configuration location or {@code null} if default
	 * initialization is required
	 * @param logFile the log output file that should be written or {@code null} for
	 * console only output
	 * @deprecated since 1.3 in favor of
	 * {@link #initialize(LoggingInitializationContext, String, LogFile)}
	 */
	@Deprecated
	public void initialize(String configLocation, LogFile logFile) {
	}

	/**
	 * Clean up the logging system. The default implementation does nothing. Subclasses
	 * should override this method to perform any logging system-specific cleanup.
	 */
	public void cleanUp() {
	}

	/**
	 * Returns a {@link Runnable} that can handle shutdown of this logging system when the
	 * JVM exits. The default implementation returns {@code null}, indicating that no
	 * shutdown is required.
	 * @return the shutdown handler, or {@code null}
	 */
	public Runnable getShutdownHandler() {
		return null;
	}

	/**
	 * Sets the logging level for a given logger.
	 * @param loggerName the name of the logger to set
	 * @param level the log level
	 */
	public abstract void setLogLevel(String loggerName, LogLevel level);

	/**
	 * Detect and return the logging system in use. Supports Logback, Log4J, Log4J2 and
	 * Java Logging.
	 * @param classLoader the classloader
	 * @return The logging system
	 */
	public static LoggingSystem get(ClassLoader classLoader) {
		String loggingSystem = System.getProperty(SYSTEM_PROPERTY);
		if (StringUtils.hasLength(loggingSystem)) {
			return get(classLoader, loggingSystem);
		}
		for (Map.Entry<String, String> entry : SYSTEMS.entrySet()) {
			if (ClassUtils.isPresent(entry.getKey(), classLoader)) {
				return get(classLoader, entry.getValue());
			}
		}
		throw new IllegalStateException("No suitable logging system located");
	}

	private static LoggingSystem get(ClassLoader classLoader, String loggingSystemClass) {
		try {
			Class<?> systemClass = ClassUtils.forName(loggingSystemClass, classLoader);
			return (LoggingSystem) systemClass.getConstructor(ClassLoader.class)
					.newInstance(classLoader);
		}
		catch (Exception ex) {
			throw new IllegalStateException(ex);
		}
	}

}
