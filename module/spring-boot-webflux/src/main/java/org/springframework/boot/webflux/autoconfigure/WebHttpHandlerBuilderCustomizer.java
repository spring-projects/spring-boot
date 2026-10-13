/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.webflux.autoconfigure;

import org.springframework.web.server.adapter.WebHttpHandlerBuilder;

/**
 * Callback interface used to customize a {@link WebHttpHandlerBuilder}.
 *
 * @author Lasse Wulff
 * @since 4.0.0
 */
@FunctionalInterface
public interface WebHttpHandlerBuilderCustomizer {

	/**
	 * Callback to customize a {@link WebHttpHandlerBuilder} instance.
	 * @param webHttpHandlerBuilder the {@link WebHttpHandlerBuilder} to customize
	 */
	void customize(WebHttpHandlerBuilder webHttpHandlerBuilder);

}
