/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.autoconfigure.context;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for
 * {@link ConfigurationProperties @ConfigurationProperties} beans. Automatically binds and
 * validates any bean annotated with {@code @ConfigurationProperties}.
 *
 * @author Stephane Nicoll
 * @since 1.3.0
 * @see EnableConfigurationProperties
 * @see ConfigurationProperties
 */
@AutoConfiguration
@EnableConfigurationProperties
public final class ConfigurationPropertiesAutoConfiguration {

}
