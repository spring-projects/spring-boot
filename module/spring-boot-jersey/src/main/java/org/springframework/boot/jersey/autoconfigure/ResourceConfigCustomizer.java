/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jersey.autoconfigure;

import org.glassfish.jersey.server.ResourceConfig;

/**
 * Callback interface that can be implemented by beans wishing to customize Jersey's
 * {@link ResourceConfig} before it is used.
 *
 * @author Eddú Meléndez
 * @since 4.0.0
 */
@FunctionalInterface
public interface ResourceConfigCustomizer {

	/**
	 * Customize the resource config.
	 * @param config the {@link ResourceConfig} to customize
	 */
	void customize(ResourceConfig config);

}
