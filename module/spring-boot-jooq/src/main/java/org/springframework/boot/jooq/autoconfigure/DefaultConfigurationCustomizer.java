/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jooq.autoconfigure;

import org.jooq.impl.DefaultConfiguration;

/**
 * Callback interface that can be implemented by beans wishing to customize the
 * {@link DefaultConfiguration} to fine-tune its auto-configuration.
 *
 * @author Stephane Nicoll
 * @since 4.0.0
 */
@FunctionalInterface
public interface DefaultConfigurationCustomizer {

	/**
	 * Customize the {@link DefaultConfiguration jOOQ Configuration}.
	 * @param configuration the configuration to customize
	 */
	void customize(DefaultConfiguration configuration);

}
