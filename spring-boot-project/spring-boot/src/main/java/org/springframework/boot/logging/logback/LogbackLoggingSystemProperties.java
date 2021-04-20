/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.logging.logback;

import java.nio.charset.Charset;
import java.util.function.BiConsumer;

import ch.qos.logback.core.util.FileSize;

import org.springframework.boot.logging.LogFile;
import org.springframework.boot.logging.LoggingSystemProperties;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.ConverterNotFoundException;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertyResolver;
import org.springframework.util.unit.DataSize;

/**
 * {@link LoggingSystemProperties} for Logback.
 *
 * @author Phillip Webb
 * @since 2.4.0
 */
public class LogbackLoggingSystemProperties extends LoggingSystemProperties {

	/**
	 * The name of the System property that contains the rolled-over log file name
	 * pattern.
	 */
	public static final String ROLLINGPOLICY_FILE_NAME_PATTERN = "LOGBACK_ROLLINGPOLICY_FILE_NAME_PATTERN";

	/**
	 * The name of the System property that contains the clean history on start flag.
	 */
	public static final String ROLLINGPOLICY_CLEAN_HISTORY_ON_START = "LOGBACK_ROLLINGPOLICY_CLEAN_HISTORY_ON_START";

	/**
	 * The name of the System property that contains the file log max size.
	 */
	public static final String ROLLINGPOLICY_MAX_FILE_SIZE = "LOGBACK_ROLLINGPOLICY_MAX_FILE_SIZE";

	/**
	 * The name of the System property that contains the file total size cap.
	 */
	public static final String ROLLINGPOLICY_TOTAL_SIZE_CAP = "LOGBACK_ROLLINGPOLICY_TOTAL_SIZE_CAP";

	/**
	 * The name of the System property that contains the file log max history.
	 */
	public static final String ROLLINGPOLICY_MAX_HISTORY = "LOGBACK_ROLLINGPOLICY_MAX_HISTORY";

	public LogbackLoggingSystemProperties(Environment environment) {
		super(environment);
	}

	/**
	 * Create a new {@link LogbackLoggingSystemProperties} instance.
	 * @param environment the source environment
	 * @param setter setter used to apply the property
	 * @since 2.4.3
	 */
	public LogbackLoggingSystemProperties(Environment environment, BiConsumer<String, String> setter) {
		super(environment, setter);
	}

	@Override
	protected Charset getDefaultCharset() {
		return Charset.defaultCharset();
	}

	@Override
	protected void apply(LogFile logFile, PropertyResolver resolver) {
		super.apply(logFile, resolver);
		applyRollingPolicy(resolver, ROLLINGPOLICY_FILE_NAME_PATTERN, "logging.logback.rollingpolicy.file-name-pattern",
				"logging.pattern.rolling-file-name");
		applyRollingPolicy(resolver, ROLLINGPOLICY_CLEAN_HISTORY_ON_START,
				"logging.logback.rollingpolicy.clean-history-on-start", "logging.file.clean-history-on-start");
		applyRollingPolicy(resolver, ROLLINGPOLICY_MAX_FILE_SIZE, "logging.logback.rollingpolicy.max-file-size",
				"logging.file.max-size", DataSize.class);
		applyRollingPolicy(resolver, ROLLINGPOLICY_TOTAL_SIZE_CAP, "logging.logback.rollingpolicy.total-size-cap",
				"logging.file.total-size-cap", DataSize.class);
		applyRollingPolicy(resolver, ROLLINGPOLICY_MAX_HISTORY, "logging.logback.rollingpolicy.max-history",
				"logging.file.max-history");
	}

	private void applyRollingPolicy(PropertyResolver resolver, String systemPropertyName, String propertyName,
			String deprecatedPropertyName) {
		applyRollingPolicy(resolver, systemPropertyName, propertyName, deprecatedPropertyName, String.class);
	}

	private <T> void applyRollingPolicy(PropertyResolver resolver, String systemPropertyName, String propertyName,
			String deprecatedPropertyName, Class<T> type) {
		T value = getProperty(resolver, propertyName, type);
		if (value == null) {
			value = getProperty(resolver, deprecatedPropertyName, type);
		}
		if (value != null) {
			String stringValue = String.valueOf((value instanceof DataSize) ? ((DataSize) value).toBytes() : value);
			setSystemProperty(systemPropertyName, stringValue);
		}
	}

	@SuppressWarnings("unchecked")
	private <T> T getProperty(PropertyResolver resolver, String key, Class<T> type) {
		try {
			return resolver.getProperty(key, type);
		}
		catch (ConversionFailedException | ConverterNotFoundException ex) {
			if (type != DataSize.class) {
				throw ex;
			}
			String value = resolver.getProperty(key);
			return (T) DataSize.ofBytes(FileSize.valueOf(value).getSize());
		}
	}

}
