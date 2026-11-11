/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.cassandra.autoconfigure;

import com.datastax.oss.driver.api.core.config.DriverConfigLoader;
import com.datastax.oss.driver.api.core.config.ProgrammaticDriverConfigLoaderBuilder;

/**
 * Callback interface that can be implemented by beans wishing to customize the
 * {@link DriverConfigLoaderBuilderCustomizer} to fine-tune its auto-configuration before
 * it creates a {@link DriverConfigLoader}.
 *
 * @author Stephane Nicoll
 * @since 4.0.0
 */
public interface DriverConfigLoaderBuilderCustomizer {

	/**
	 * Customize the {@linkplain ProgrammaticDriverConfigLoaderBuilder DriverConfigLoader
	 * builder}.
	 * @param builder the builder to customize
	 */
	void customize(ProgrammaticDriverConfigLoaderBuilder builder);

}
