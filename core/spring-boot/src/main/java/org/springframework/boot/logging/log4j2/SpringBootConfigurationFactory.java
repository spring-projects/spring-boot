/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.logging.log4j2;

import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.DefaultConfiguration;
import org.apache.logging.log4j.core.config.Order;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.jspecify.annotations.Nullable;

/**
 * Spring Boot {@link ConfigurationFactory} that customizes Log4J2's default configuration
 * to:
 *
 * <ol>
 * <li>Prevent logger warnings from being printed when the application first starts.
 * </ol>
 *
 * This factory is ordered last and is triggered by a {@code log4j2.springboot} classpath
 * resource (which is bundled in this jar). If the {@link Log4J2LoggingSystem} is active,
 * a {@link DefaultConfiguration} is returned with the expectation that the system will
 * later re-initialize Log4J2 with the correct configuration file.
 *
 * @author Phillip Webb
 * @since 1.5.0
 */
@Plugin(name = "SpringBootConfigurationFactory", category = ConfigurationFactory.CATEGORY)
@Order(0)
public class SpringBootConfigurationFactory extends ConfigurationFactory {

	private static final String[] TYPES = { ".springboot" };

	@Override
	protected String[] getSupportedTypes() {
		return TYPES;
	}

	@Override
	public @Nullable Configuration getConfiguration(LoggerContext loggerContext, @Nullable ConfigurationSource source) {
		if (source == null || source == ConfigurationSource.NULL_SOURCE) {
			return null;
		}
		return new DefaultConfiguration();
	}

}
