/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.validation.autoconfigure;

import jakarta.validation.Configuration;

/**
 * Callback interface that can be used to customize {@link Configuration}.
 *
 * @author Dang Zhicairang
 * @since 4.0.0
 */
@FunctionalInterface
public interface ValidationConfigurationCustomizer {

	/**
	 * Customize the given {@code configuration}.
	 * @param configuration the configuration to customize
	 */
	void customize(Configuration<?> configuration);

}
