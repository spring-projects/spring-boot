/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jersey.autoconfigure.actuate.web;

import org.glassfish.jersey.server.ResourceConfig;

/**
 * Callback interface that can be implemented by beans wishing to customize Jersey's
 * {@link ResourceConfig} in the management context before it is used.
 *
 * @author Andy Wilkinson
 * @since 4.0.0
 */
public interface ManagementContextResourceConfigCustomizer {

	/**
	 * Customize the resource config.
	 * @param config the {@link ResourceConfig} to customize
	 */
	void customize(ResourceConfig config);

}
