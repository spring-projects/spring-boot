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

package org.springframework.bootstrap.logging;

import org.springframework.util.ClassUtils;

/**
 * Common abstraction over logging systems.
 * 
 * @author Phillip Webb
 * @author Dave Syer
 */
public abstract class LoggingSystem {

	/**
	 * Reset the logging system to be limit output. This method may be called before
	 * {@link #initialize()} to reduce logging noise until the systems has been full
	 * Initialized.
	 */
	public abstract void beforeInitialize();

	/**
	 * Initialize the logging system using sensible defaults. This method should generally
	 * try to find system specific configuration on classpath before falling back to
	 * sensible defaults.
	 */
	public abstract void initialize();

	/**
	 * Initialize the logging system from a logging configuration location.
	 * @param configLocation a log configuration location
	 */
	public abstract void initialize(String configLocation);

	/**
	 * Detect and return the logging system in use.
	 * @return The logging system
	 */
	public static LoggingSystem get(ClassLoader classLoader) {
		if (ClassUtils.isPresent("ch.qos.logback.core.Appender", classLoader)) {
			return new LogbackLoggingSystem(classLoader);
		}
		if (ClassUtils.isPresent("org.apache.log4j.PropertyConfigurator", classLoader)) {
			return new Log4JLoggingSystem(classLoader);
		}
		return new JavaLoggingSystem(classLoader);
	}

}
