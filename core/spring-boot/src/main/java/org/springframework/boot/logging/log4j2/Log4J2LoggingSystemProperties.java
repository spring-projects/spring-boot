
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
 * {@link Log4J2LoggingSystemProperties} for Log4j2.
 *
 * @see Log4J2RollingPolicySystemProperty
 * @author Andrey Timonin
 * @since 4.1.0
 */
public class Log4J2LoggingSystemProperties extends LoggingSystemProperties {

	public Log4J2LoggingSystemProperties(Environment environment) {
		super(environment);
	}

	/**
	 * Create a new {@link Log4J2LoggingSystemProperties} instance.
	 * @param environment the source environment
	 * @param setter setter used to apply the property
	 */
	public Log4J2LoggingSystemProperties(Environment environment,
			@Nullable BiConsumer<String, @Nullable String> setter) {
		super(environment, setter);
	}

	/**
	 * Create a new {@link Log4J2LoggingSystemProperties} instance.
	 * @param environment the source environment
	 * @param defaultValueResolver function used to resolve default values or {@code null}
	 * @param setter setter used to apply the property or {@code null} for system
	 * properties
	 */
	public Log4J2LoggingSystemProperties(Environment environment,
			@Nullable Function<@Nullable String, @Nullable String> defaultValueResolver,
			@Nullable BiConsumer<String, @Nullable String> setter) {
		super(environment, defaultValueResolver, setter);
	}

	@Override
	protected void apply(@Nullable LogFile logFile, PropertyResolver resolver) {
		super.apply(logFile, resolver);
		applyRollingPolicyProperties(resolver);
	}

	private void applyRollingPolicyProperties(PropertyResolver resolver) {
		applyRollingPolicy(Log4J2RollingPolicySystemProperty.FILE_NAME_PATTERN, resolver);
		applyRollingPolicy(Log4J2RollingPolicySystemProperty.MAX_FILE_SIZE, resolver, DataSize.class);
		applyRollingPolicy(Log4J2RollingPolicySystemProperty.TOTAL_SIZE_CAP, resolver, DataSize.class);
		applyRollingPolicy(Log4J2RollingPolicySystemProperty.MAX_HISTORY, resolver);
	}

	private void applyRollingPolicy(Log4J2RollingPolicySystemProperty property, PropertyResolver resolver) {
		applyRollingPolicy(property, resolver, String.class);
	}

	private <T> void applyRollingPolicy(Log4J2RollingPolicySystemProperty property, PropertyResolver resolver,
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
			return (T) DataSize.parse(value);
		}
	}

}