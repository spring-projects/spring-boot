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
 * @deprecated since 4.2.0 for removal in 4.4.0 in favor of
 * {@code org.springframework.boot.webmvc.autoconfigure.ForwardedHeaderFilterCustomizer}.
 */
@Deprecated(since = "4.2.0", forRemoval = true)
public interface ForwardedHeaderFilterCustomizer {

	/**
	 * Customizes the given {@code filter}.
	 * @param filter the filter to customize
	 */
	void customize(ForwardedHeaderFilter filter);

}
