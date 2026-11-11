/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.integration.autoconfigure;

import org.springframework.integration.scheduling.PollerMetadata;

/**
 * Callback interface that can be implemented by beans wishing to customize the
 * {@link PollerMetadata} to fine-tune its auto-configuration.
 *
 * @author Yanming Zhou
 * @since 4.0.0
 */
@FunctionalInterface
public interface PollerMetadataCustomizer {

	/**
	 * Customize the {@link PollerMetadata}.
	 * @param pollerMetadata the {@code PollerMetadata} to customize
	 */
	void customize(PollerMetadata pollerMetadata);

}
