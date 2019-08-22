/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.logging;

import org.springframework.boot.system.ApplicationPid;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertyResolver;
import org.springframework.core.env.PropertySourcesPropertyResolver;
import org.springframework.util.Assert;

/**
 * Utility to set system properties that can later be used by log configuration files.
 *
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @author Madhura Bhave
 * @author Vedran Pavic
 * @author Robert Thornton
 * @since 2.0.0
 */
public class LoggingSystemProperties {

	/**
	 * The name of the System property that contains the process ID.
	 */
	public static final String PID_KEY = "PID";

	/**
	 * The name of the System property that contains the exception conversion word.
	 */
	public static final String EXCEPTION_CONVERSION_WORD = "LOG_EXCEPTION_CONVERSION_WORD";

	/**
	 * The name of the System property that contains the log file.
	 */
	public static final String LOG_FILE = "LOG_FILE";

	/**
	 * The name of the System property that contains the log path.
	 */
	public static final String LOG_PATH = "LOG_PATH";

	/**
	 * The name of the System property that contains the console log pattern.
	 */
	public static final String CONSOLE_LOG_PATTERN = "CONSOLE_LOG_PATTERN";

	/**
	 * The name of the System property that contains the clean history on start flag.
	 */
	public static final String FILE_CLEAN_HISTORY_ON_START = "LOG_FILE_CLEAN_HISTORY_ON_START";

	/**
	 * The name of the System property that contains the file log pattern.
	 */
	public static final String FILE_LOG_PATTERN = "FILE_LOG_PATTERN";

	/**
	 * The name of the System property that contains the file log max history.
	 */
	public static final String FILE_MAX_HISTORY = "LOG_FILE_MAX_HISTORY";

	/**
	 * The name of the System property that contains the file log max size.
	 */
	public static final String FILE_MAX_SIZE = "LOG_FILE_MAX_SIZE";

	/**
	 * The name of the System property that contains the file total size cap.
	 */
	public static final String FILE_TOTAL_SIZE_CAP = "LOG_FILE_TOTAL_SIZE_CAP";

	/**
	 * The name of the System property that contains the log level pattern.
	 */
	public static final String LOG_LEVEL_PATTERN = "LOG_LEVEL_PATTERN";

	/**
	 * The name of the System property that contains the log date-format pattern.
	 */
	public static final String LOG_DATEFORMAT_PATTERN = "LOG_DATEFORMAT_PATTERN";

	private final Environment environment;

	/**
	 * Create a new {@link LoggingSystemProperties} instance.
	 * @param environment the source environment
	 */
	public LoggingSystemProperties(Environment environment) {
		Assert.notNull(environment, "Environment must not be null");
		this.environment = environment;
	}

	public void apply() {
		apply(null);
	}

	public void apply(LogFile logFile) {
		PropertyResolver resolver = getPropertyResolver();
		setSystemProperty(resolver, EXCEPTION_CONVERSION_WORD, "exception-conversion-word");
		setSystemProperty(PID_KEY, new ApplicationPid().toString());
		setSystemProperty(resolver, CONSOLE_LOG_PATTERN, "pattern.console");
		setSystemProperty(resolver, FILE_LOG_PATTERN, "pattern.file");
		setSystemProperty(resolver, FILE_CLEAN_HISTORY_ON_START, "file.clean-history-on-start");
		setSystemProperty(resolver, FILE_MAX_HISTORY, "file.max-history");
		setSystemProperty(resolver, FILE_MAX_SIZE, "file.max-size");
		setSystemProperty(resolver, FILE_TOTAL_SIZE_CAP, "file.total-size-cap");
		setSystemProperty(resolver, LOG_LEVEL_PATTERN, "pattern.level");
		setSystemProperty(resolver, LOG_DATEFORMAT_PATTERN, "pattern.dateformat");
		if (logFile != null) {
			logFile.applyToSystemProperties();
		}
	}

	private PropertyResolver getPropertyResolver() {
		if (this.environment instanceof ConfigurableEnvironment) {
			PropertySourcesPropertyResolver resolver = new PropertySourcesPropertyResolver(
					((ConfigurableEnvironment) this.environment).getPropertySources());
			resolver.setIgnoreUnresolvableNestedPlaceholders(true);
			return resolver;
		}
		return this.environment;
	}

	private void setSystemProperty(PropertyResolver resolver, String systemPropertyName, String propertyName) {
		setSystemProperty(systemPropertyName, resolver.getProperty("logging." + propertyName));
	}

	private void setSystemProperty(String name, String value) {
		if (System.getProperty(name) == null && value != null) {
			System.setProperty(name, value);
		}
	}

}
