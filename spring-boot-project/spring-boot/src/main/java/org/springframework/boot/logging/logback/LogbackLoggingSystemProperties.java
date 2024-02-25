/*
 * Copyright 2012-2023 the original author or authors.
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
import java.util.function.Function;

import ch.qos.logback.core.util.FileSize;

import org.springframework.boot.logging.LogFile;
import org.springframework.boot.logging.LoggingSystemProperties;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.ConverterNotFoundException;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertyResolver;
import org.springframework.util.ClassUtils;
import org.springframework.util.unit.DataSize;

/**
 * {@link LoggingSystemProperties} for Logback.
 *
 * @author Phillip Webb
 * @since 2.4.0
 * @see RollingPolicySystemProperty
 */
public class LogbackLoggingSystemProperties extends LoggingSystemProperties {

	private static final boolean JBOSS_LOGGING_PRESENT = ClassUtils.isPresent("org.jboss.logging.Logger",
			LogbackLoggingSystemProperties.class.getClassLoader());

	/**
	 * The name of the System property that contains the rolled-over log file name
	 * pattern.
	 * @deprecated since 3.2.0 for removal in 3.4.0 in favor of calling
	 * {@link RollingPolicySystemProperty#getEnvironmentVariableName()} on
	 * {@link RollingPolicySystemProperty#FILE_NAME_PATTERN}
	 */
	@Deprecated(since = "3.2.0", forRemoval = true)
	public static final String ROLLINGPOLICY_FILE_NAME_PATTERN = RollingPolicySystemProperty.FILE_NAME_PATTERN
		.getEnvironmentVariableName();

	/**
	 * The name of the System property that contains the clean history on start flag.
	 * @deprecated since 3.2.0 for removal in 3.4.0 in favor of calling
	 * {@link RollingPolicySystemProperty#getEnvironmentVariableName()} on
	 * {@link RollingPolicySystemProperty#CLEAN_HISTORY_ON_START}
	 */
	@Deprecated(since = "3.2.0", forRemoval = true)
	public static final String ROLLINGPOLICY_CLEAN_HISTORY_ON_START = RollingPolicySystemProperty.CLEAN_HISTORY_ON_START
		.getEnvironmentVariableName();

	/**
	 * The name of the System property that contains the file log max size.
	 * @deprecated since 3.2.0 for removal in 3.4.0 in favor of calling
	 * {@link RollingPolicySystemProperty#getEnvironmentVariableName()} on
	 * {@link RollingPolicySystemProperty#MAX_FILE_SIZE}
	 */
	@Deprecated(since = "3.2.0", forRemoval = true)
	public static final String ROLLINGPOLICY_MAX_FILE_SIZE = RollingPolicySystemProperty.MAX_FILE_SIZE
		.getEnvironmentVariableName();

	/**
	 * The name of the System property that contains the file total size cap.
	 * @deprecated since 3.2.0 for removal in 3.4.0 in favor of calling
	 * {@link RollingPolicySystemProperty#getEnvironmentVariableName()} on
	 * {@link RollingPolicySystemProperty#TOTAL_SIZE_CAP}
	 */
	@Deprecated(since = "3.2.0", forRemoval = true)
	public static final String ROLLINGPOLICY_TOTAL_SIZE_CAP = RollingPolicySystemProperty.TOTAL_SIZE_CAP
		.getEnvironmentVariableName();

	/**
	 * The name of the System property that contains the file log max history.
	 * @deprecated since 3.2.0 for removal in 3.4.0 in favor of calling
	 * {@link RollingPolicySystemProperty#getEnvironmentVariableName()} on
	 * {@link RollingPolicySystemProperty#MAX_HISTORY}
	 */
	@Deprecated(since = "3.2.0", forRemoval = true)
	public static final String ROLLINGPOLICY_MAX_HISTORY = RollingPolicySystemProperty.MAX_HISTORY
		.getEnvironmentVariableName();

	/**
     * Constructs a new instance of LogbackLoggingSystemProperties with the specified environment.
     *
     * @param environment the environment to be used by the logging system properties
     */
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

	/**
	 * Create a new {@link LoggingSystemProperties} instance.
	 * @param environment the source environment
	 * @param defaultValueResolver function used to resolve default values or {@code null}
	 * @param setter setter used to apply the property or {@code null} for system
	 * properties
	 * @since 3.2.0
	 */
	public LogbackLoggingSystemProperties(Environment environment, Function<String, String> defaultValueResolver,
			BiConsumer<String, String> setter) {
		super(environment, defaultValueResolver, setter);
	}

	/**
     * Returns the default charset used by the system.
     * 
     * @return The default charset used by the system.
     */
    @Override
	protected Charset getDefaultCharset() {
		return Charset.defaultCharset();
	}

	/**
     * Applies the LogFile and PropertyResolver to the LogbackLoggingSystemProperties.
     * 
     * @param logFile the LogFile to apply
     * @param resolver the PropertyResolver to apply
     */
    @Override
	protected void apply(LogFile logFile, PropertyResolver resolver) {
		super.apply(logFile, resolver);
		applyJBossLoggingProperties();
		applyRollingPolicyProperties(resolver);
	}

	/**
     * Applies JBoss logging properties if JBoss logging is present.
     * Sets the system property "org.jboss.logging.provider" to "slf4j".
     */
    private void applyJBossLoggingProperties() {
		if (JBOSS_LOGGING_PRESENT) {
			setSystemProperty("org.jboss.logging.provider", "slf4j");
		}
	}

	/**
     * Applies the rolling policy properties to the given property resolver.
     * 
     * @param resolver the property resolver to apply the rolling policy properties to
     */
    private void applyRollingPolicyProperties(PropertyResolver resolver) {
		applyRollingPolicy(RollingPolicySystemProperty.FILE_NAME_PATTERN, resolver);
		applyRollingPolicy(RollingPolicySystemProperty.CLEAN_HISTORY_ON_START, resolver);
		applyRollingPolicy(RollingPolicySystemProperty.MAX_FILE_SIZE, resolver, DataSize.class);
		applyRollingPolicy(RollingPolicySystemProperty.TOTAL_SIZE_CAP, resolver, DataSize.class);
		applyRollingPolicy(RollingPolicySystemProperty.MAX_HISTORY, resolver);
	}

	/**
     * Applies the rolling policy to the specified property using the given resolver.
     * 
     * @param property the rolling policy system property to apply
     * @param resolver the property resolver to use
     */
    private void applyRollingPolicy(RollingPolicySystemProperty property, PropertyResolver resolver) {
		applyRollingPolicy(property, resolver, String.class);
	}

	/**
     * Applies the rolling policy system property by resolving the property value and setting it as a system property.
     * 
     * @param property the rolling policy system property to apply
     * @param resolver the property resolver to use for resolving the property value
     * @param type the type of the property value
     * @param <T> the generic type of the property value
     */
    private <T> void applyRollingPolicy(RollingPolicySystemProperty property, PropertyResolver resolver,
			Class<T> type) {
		T value = getProperty(resolver, property.getApplicationPropertyName(), type);
		value = (value != null) ? value : getProperty(resolver, property.getDeprecatedApplicationPropertyName(), type);
		if (value != null) {
			String stringValue = String.valueOf((value instanceof DataSize dataSize) ? dataSize.toBytes() : value);
			setSystemProperty(property.getEnvironmentVariableName(), stringValue);
		}
	}

	/**
     * Retrieves a property value from the given PropertyResolver using the specified key and type.
     * 
     * @param resolver the PropertyResolver to retrieve the property value from
     * @param key the key of the property to retrieve
     * @param type the type of the property value
     * @return the property value of the specified type
     * @throws ConversionFailedException if the property value cannot be converted to the specified type
     * @throws ConverterNotFoundException if a converter for the specified type is not found
     * @throws IllegalArgumentException if the type is not supported and is not DataSize
     * @throws NullPointerException if the resolver or key is null
     */
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
