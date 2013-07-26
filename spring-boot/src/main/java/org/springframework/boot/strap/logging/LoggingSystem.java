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

package org.springframework.boot.strap.logging;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.util.ClassUtils;

/**
 * Common abstraction over logging systems.
 * 
 * @author Phillip Webb
 * @author Dave Syer
 */
public abstract class LoggingSystem {

	private static final Map<String, String> SYSTEMS;
	static {
		Map<String, String> systems = new LinkedHashMap<String, String>();
		String pkg = LoggingSystem.class.getPackage().getName();
		systems.put("ch.qos.logback.core.Appender", pkg + ".logback.LogbackLoggingSystem");
		systems.put("org.apache.log4j.PropertyConfigurator", pkg
				+ ".log4j.Log4JLoggingSystem");
		systems.put("java.util.logging.LogManager", pkg + ".java.JavaLoggingSystem");
		SYSTEMS = Collections.unmodifiableMap(systems);
	}

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
		for (Map.Entry<String, String> entry : SYSTEMS.entrySet()) {
			if (ClassUtils.isPresent(entry.getKey(), classLoader)) {
				try {
					Class<?> systemClass = ClassUtils.forName(entry.getValue(),
							classLoader);
					return (LoggingSystem) systemClass.getConstructor(ClassLoader.class)
							.newInstance(classLoader);
				}
				catch (Exception ex) {
					throw new IllegalStateException(ex);
				}
			}
		}
		throw new IllegalStateException("No suitable logging system located");
	}

}
