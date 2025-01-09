/*
 * Copyright 2012-2025 the original author or authors.
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
import java.util.function.Function;

import org.springframework.boot.system.ApplicationPid;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertyResolver;
import org.springframework.core.env.PropertySourcesPropertyResolver;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Utility to set system properties that can later be used by log configuration files.
 *
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @author Madhura Bhave
 * @author Vedran Pavic
 * @author Robert Thornton
 * @author Eddú Meléndez
 * @author Jonatan Ivanov
 * @since 2.0.0
 * @see LoggingSystemProperty
 */
public class LoggingSystemProperties {

	private static final BiConsumer<String, String> systemPropertySetter = (name, value) -> {
		if (System.getProperty(name) == null && value != null) {
			System.setProperty(name, value);
		}
	};

	private final Environment environment;

	private final Function<String, String> defaultValueResolver;

	private final BiConsumer<String, String> setter;

	/**
	 * Create a new {@link LoggingSystemProperties} instance.
	 * @param environment the source environment
	 */
	public LoggingSystemProperties(Environment environment) {
		this(environment, null);
	}

	/**
	 * Create a new {@link LoggingSystemProperties} instance.
	 * @param environment the source environment
	 * @param setter setter used to apply the property or {@code null} for system
	 * properties
	 * @since 2.4.2
	 */
	public LoggingSystemProperties(Environment environment, BiConsumer<String, String> setter) {
		this(environment, null, setter);
	}

	/**
	 * Create a new {@link LoggingSystemProperties} instance.
	 * @param environment the source environment
	 * @param defaultValueResolver function used to resolve default values or {@code null}
	 * @param setter setter used to apply the property or {@code null} for system
	 * properties
	 * @since 3.2.0
	 */
	public LoggingSystemProperties(Environment environment, Function<String, String> defaultValueResolver,
			BiConsumer<String, String> setter) {
		Assert.notNull(environment, "'environment' must not be null");
		this.environment = environment;
		this.defaultValueResolver = (defaultValueResolver != null) ? defaultValueResolver : (name) -> null;
		this.setter = (setter != null) ? setter : systemPropertySetter;
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

	private PropertyResolver getPropertyResolver() {
		if (this.environment instanceof ConfigurableEnvironment configurableEnvironment) {
			PropertySourcesPropertyResolver resolver = new PropertySourcesPropertyResolver(
					configurableEnvironment.getPropertySources());
			resolver.setConversionService(configurableEnvironment.getConversionService());
			resolver.setIgnoreUnresolvableNestedPlaceholders(true);
			return resolver;
		}
		return this.environment;
	}

	protected void apply(LogFile logFile, PropertyResolver resolver) {
		String defaultCharsetName = getDefaultCharset().name();
		setSystemProperty(LoggingSystemProperty.APPLICATION_NAME, resolver);
		setSystemProperty(LoggingSystemProperty.APPLICATION_GROUP, resolver);
		setSystemProperty(LoggingSystemProperty.PID, new ApplicationPid().toString());
		setSystemProperty(LoggingSystemProperty.CONSOLE_CHARSET, resolver, defaultCharsetName);
		setSystemProperty(LoggingSystemProperty.FILE_CHARSET, resolver, defaultCharsetName);
		setSystemProperty(LoggingSystemProperty.CONSOLE_THRESHOLD, resolver, this::thresholdMapper);
		setSystemProperty(LoggingSystemProperty.FILE_THRESHOLD, resolver, this::thresholdMapper);
		setSystemProperty(LoggingSystemProperty.EXCEPTION_CONVERSION_WORD, resolver);
		setSystemProperty(LoggingSystemProperty.CONSOLE_PATTERN, resolver);
		setSystemProperty(LoggingSystemProperty.FILE_PATTERN, resolver);
		setSystemProperty(LoggingSystemProperty.CONSOLE_STRUCTURED_FORMAT, resolver);
		setSystemProperty(LoggingSystemProperty.FILE_STRUCTURED_FORMAT, resolver);
		setSystemProperty(LoggingSystemProperty.LEVEL_PATTERN, resolver);
		setSystemProperty(LoggingSystemProperty.DATEFORMAT_PATTERN, resolver);
		setSystemProperty(LoggingSystemProperty.CORRELATION_PATTERN, resolver);
		if (logFile != null) {
			logFile.applyToSystemProperties();
		}
	}

	private void setSystemProperty(LoggingSystemProperty property, PropertyResolver resolver) {
		setSystemProperty(property, resolver, Function.identity());
	}

	private void setSystemProperty(LoggingSystemProperty property, PropertyResolver resolver,
			Function<String, String> mapper) {
		setSystemProperty(property, resolver, null, mapper);
	}

	private void setSystemProperty(LoggingSystemProperty property, PropertyResolver resolver, String defaultValue) {
		setSystemProperty(property, resolver, defaultValue, Function.identity());
	}

	private void setSystemProperty(LoggingSystemProperty property, PropertyResolver resolver, String defaultValue,
			Function<String, String> mapper) {
		if (property.getIncludePropertyName() != null) {
			if (!resolver.getProperty(property.getIncludePropertyName(), Boolean.class, Boolean.TRUE)) {
				return;
			}
		}
		String value = (property.getApplicationPropertyName() != null)
				? resolver.getProperty(property.getApplicationPropertyName()) : null;
		value = (value != null) ? value : this.defaultValueResolver.apply(property.getApplicationPropertyName());
		value = (value != null) ? value : defaultValue;
		value = mapper.apply(value);
		setSystemProperty(property.getEnvironmentVariableName(), value);
		if (property == LoggingSystemProperty.APPLICATION_NAME && StringUtils.hasText(value)) {
			// LOGGED_APPLICATION_NAME is deprecated for removal in 3.6.0
			setSystemProperty("LOGGED_APPLICATION_NAME", "[%s] ".formatted(value));
		}
	}

	private void setSystemProperty(LoggingSystemProperty property, String value) {
		setSystemProperty(property.getEnvironmentVariableName(), value);
	}

	private String thresholdMapper(String input) {
		// YAML converts an unquoted OFF to false
		if ("false".equals(input)) {
			return "OFF";
		}
		return input;
	}

	/**
	 * Set a system property.
	 * @param name the property name
	 * @param value the value
	 */
	protected final void setSystemProperty(String name, String value) {
		this.setter.accept(name, value);
	}

}
