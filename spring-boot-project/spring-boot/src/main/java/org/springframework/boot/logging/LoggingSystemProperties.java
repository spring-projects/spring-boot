/*
 * Copyright 2012-2022 the original author or authors.
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

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.function.BiConsumer;

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
 * @author Eddú Meléndez
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
	 * The name of the System property that contains the console log charset.
	 */
	public static final String CONSOLE_LOG_CHARSET = "CONSOLE_LOG_CHARSET";

	/**
	 * The name of the System property that contains the file log pattern.
	 */
	public static final String FILE_LOG_PATTERN = "FILE_LOG_PATTERN";

	/**
	 * The name of the System property that contains the file log charset.
	 */
	public static final String FILE_LOG_CHARSET = "FILE_LOG_CHARSET";

	/**
	 * The name of the System property that contains the log level pattern.
	 */
	public static final String LOG_LEVEL_PATTERN = "LOG_LEVEL_PATTERN";

	/**
	 * The name of the System property that contains the log date-format pattern.
	 */
	public static final String LOG_DATEFORMAT_PATTERN = "LOG_DATEFORMAT_PATTERN";

	private static final BiConsumer<String, String> systemPropertySetter = (name, value) -> {
		if (System.getProperty(name) == null && value != null) {
			System.setProperty(name, value);
		}
	};

	private final Environment environment;

	private final BiConsumer<String, String> setter;

	/**
	 * Create a new {@link LoggingSystemProperties} instance.
	 * @param environment the source environment
	 */
	public LoggingSystemProperties(Environment environment) {
		this(environment, systemPropertySetter);
	}

	/**
	 * Create a new {@link LoggingSystemProperties} instance.
	 * @param environment the source environment
	 * @param setter setter used to apply the property
	 * @since 2.4.2
	 */
	public LoggingSystemProperties(Environment environment, BiConsumer<String, String> setter) {
		Assert.notNull(environment, "Environment must not be null");
		Assert.notNull(setter, "Setter must not be null");
		this.environment = environment;
		this.setter = setter;
	}

	protected Charset getDefaultCharset() {
		return StandardCharsets.UTF_8;
	}

	public final void apply() {
		apply(null);
	}

	public final void apply(LogFile logFile) {
		PropertyResolver resolver = getPropertyResolver();
		apply(logFile, resolver);
	}

	protected void apply(LogFile logFile, PropertyResolver resolver) {
		setSystemProperty(resolver, EXCEPTION_CONVERSION_WORD, "logging.exception-conversion-word");
		setSystemProperty(PID_KEY, new ApplicationPid().toString());
		setSystemProperty(resolver, CONSOLE_LOG_PATTERN, "logging.pattern.console");
		setSystemProperty(resolver, CONSOLE_LOG_CHARSET, "logging.charset.console", getDefaultCharset().name());
		setSystemProperty(resolver, LOG_DATEFORMAT_PATTERN, "logging.pattern.dateformat");
		setSystemProperty(resolver, FILE_LOG_PATTERN, "logging.pattern.file");
		setSystemProperty(resolver, FILE_LOG_CHARSET, "logging.charset.file", getDefaultCharset().name());
		setSystemProperty(resolver, LOG_LEVEL_PATTERN, "logging.pattern.level");
		if (logFile != null) {
			logFile.applyToSystemProperties();
		}
	}

	private PropertyResolver getPropertyResolver() {
		if (this.environment instanceof ConfigurableEnvironment configurableEnvironment) {
			PropertySourcesPropertyResolver resolver = new PropertySourcesPropertyResolver(
					configurableEnvironment.getPropertySources());
			resolver.setConversionService(((ConfigurableEnvironment) this.environment).getConversionService());
			resolver.setIgnoreUnresolvableNestedPlaceholders(true);
			return resolver;
		}
		return this.environment;
	}

	protected final void setSystemProperty(PropertyResolver resolver, String systemPropertyName, String propertyName) {
		setSystemProperty(resolver, systemPropertyName, propertyName, null);
	}

	protected final void setSystemProperty(PropertyResolver resolver, String systemPropertyName, String propertyName,
			String defaultValue) {
		String value = resolver.getProperty(propertyName);
		value = (value != null) ? value : defaultValue;
		setSystemProperty(systemPropertyName, value);
	}

	protected final void setSystemProperty(String name, String value) {
		this.setter.accept(name, value);
	}

}
