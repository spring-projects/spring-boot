/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.logging.log4j2;

import java.util.function.BiConsumer;
import java.util.function.Function;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.logging.LogFile;
import org.springframework.boot.logging.LoggingSystemProperties;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.ConverterNotFoundException;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertyResolver;
import org.springframework.util.unit.DataSize;

/**
 * {@link LoggingSystemProperties} for Log4j2.
 *
 * @author HoJoo Moon
 * @since 4.1.0
 * @see RollingPolicySystemProperty
 */
public class Log4j2LoggingSystemProperties extends LoggingSystemProperties {

	public Log4j2LoggingSystemProperties(Environment environment) {
		super(environment);
	}

	/**
	 * Create a new {@link Log4j2LoggingSystemProperties} instance.
	 * @param environment the source environment
	 * @param defaultValueResolver function used to resolve default values or {@code null}
	 * @param setter setter used to apply the property or {@code null} for system
	 * properties
	 */
	public Log4j2LoggingSystemProperties(Environment environment,
			Function<@Nullable String, @Nullable String> defaultValueResolver,
			@Nullable BiConsumer<String, @Nullable String> setter) {
		super(environment, defaultValueResolver, setter);
	}

	@Override
	protected void apply(@Nullable LogFile logFile, PropertyResolver resolver) {
		super.apply(logFile, resolver);
		applyRollingPolicyProperties(resolver);
	}

	private void applyRollingPolicyProperties(PropertyResolver resolver) {
		applyRollingPolicy(RollingPolicySystemProperty.FILE_NAME_PATTERN, resolver);
		applyRollingPolicy(RollingPolicySystemProperty.MAX_FILE_SIZE, resolver, DataSize.class);
		applyRollingPolicy(RollingPolicySystemProperty.MAX_HISTORY, resolver);
		applyRollingPolicy(RollingPolicySystemProperty.STRATEGY, resolver);
		applyRollingPolicy(RollingPolicySystemProperty.TIME_INTERVAL, resolver, Integer.class);
		applyRollingPolicy(RollingPolicySystemProperty.TIME_MODULATE, resolver, Boolean.class);
		applyRollingPolicy(RollingPolicySystemProperty.CRON, resolver);
	}

	private void applyRollingPolicy(RollingPolicySystemProperty property, PropertyResolver resolver) {
		applyRollingPolicy(property, resolver, String.class);
	}

	private <T> void applyRollingPolicy(RollingPolicySystemProperty property, PropertyResolver resolver,
			Class<T> type) {
		T value = getProperty(resolver, property.getApplicationPropertyName(), type);
		if (value != null) {
			String stringValue = String.valueOf((value instanceof DataSize dataSize) ? dataSize.toBytes() : value);
			setSystemProperty(property.getEnvironmentVariableName(), stringValue);
		}
	}

	@SuppressWarnings("unchecked")
	private <T> @Nullable T getProperty(PropertyResolver resolver, String key, Class<T> type) {
		try {
			return resolver.getProperty(key, type);
		}
		catch (ConversionFailedException | ConverterNotFoundException ex) {
			if (type != DataSize.class) {
				throw ex;
			}
			String value = resolver.getProperty(key);
			return (value != null) ? (T) DataSize.parse(value) : null;
		}
	}

}
