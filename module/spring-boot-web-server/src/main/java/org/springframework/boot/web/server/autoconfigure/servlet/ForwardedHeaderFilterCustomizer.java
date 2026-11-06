/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.web.server.autoconfigure.servlet;

import org.springframework.web.filter.ForwardedHeaderFilter;

/**
 * Customizer for the auto-configured {@link ForwardedHeaderFilter}.
 *
 * @author Andy Wilkinson
 * @since 4.0.0
 */
public interface ForwardedHeaderFilterCustomizer {

	/**
	 * Customizes the given {@code filter}.
	 * @param filter the filter to customize
	 */
	void customize(ForwardedHeaderFilter filter);

}
