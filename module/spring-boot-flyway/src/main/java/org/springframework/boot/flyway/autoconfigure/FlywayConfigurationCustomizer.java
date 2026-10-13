/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.flyway.autoconfigure;

import org.flywaydb.core.api.configuration.FluentConfiguration;

/**
 * Callback interface that can be implemented by beans wishing to customize the flyway
 * configuration.
 *
 * @author Stephane Nicoll
 * @since 4.0.0
 */
@FunctionalInterface
public interface FlywayConfigurationCustomizer {

	/**
	 * Customize the flyway configuration.
	 * @param configuration the {@link FluentConfiguration} to customize
	 */
	void customize(FluentConfiguration configuration);

}
