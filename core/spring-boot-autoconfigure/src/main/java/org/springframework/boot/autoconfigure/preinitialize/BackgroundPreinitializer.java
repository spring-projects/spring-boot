/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.autoconfigure.preinitialize;

/**
 * Interface used to preinitialize in the background code that may otherwise cause a delay
 * when first called. Implementations should be registered in {@code spring.factories}.
 *
 * @author Phillip Webb
 * @since 4.0.0
 */
@FunctionalInterface
public interface BackgroundPreinitializer {

	/**
	 * Perform any require preinitialization.
	 * @throws Exception on any initialization error
	 */
	void preinitialize() throws Exception;

}
