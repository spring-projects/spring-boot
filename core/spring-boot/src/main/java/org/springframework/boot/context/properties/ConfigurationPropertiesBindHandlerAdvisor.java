/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.context.properties;

import org.springframework.boot.context.properties.bind.AbstractBindHandler;
import org.springframework.boot.context.properties.bind.BindHandler;

/**
 * Allows additional functionality to be applied to the {@link BindHandler} used by the
 * {@link ConfigurationPropertiesBindingPostProcessor}.
 *
 * @author Phillip Webb
 * @since 2.1.0
 * @see AbstractBindHandler
 */
@FunctionalInterface
public interface ConfigurationPropertiesBindHandlerAdvisor {

	/**
	 * Apply additional functionality to the source bind handler.
	 * @param bindHandler the source bind handler
	 * @return a replacement bind handler that delegates to the source and provides
	 * additional functionality
	 */
	BindHandler apply(BindHandler bindHandler);

}
