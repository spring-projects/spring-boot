/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.neo4j.autoconfigure;

import org.neo4j.driver.Config;
import org.neo4j.driver.Config.ConfigBuilder;

/**
 * Callback interface that can be implemented by beans wishing to customize the a
 * {@link ConfigBuilder} to fine-tune its auto-configuration before it creates a
 * {@link Config} instance.
 *
 * @author Stephane Nicoll
 * @since 4.0.0
 */
@FunctionalInterface
public interface ConfigBuilderCustomizer {

	/**
	 * Customize the {@link ConfigBuilder}.
	 * @param configBuilder the {@link ConfigBuilder} to customize
	 */
	void customize(ConfigBuilder configBuilder);

}
