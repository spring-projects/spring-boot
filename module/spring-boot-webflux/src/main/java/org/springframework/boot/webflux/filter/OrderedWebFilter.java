/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.webflux.filter;

import org.springframework.core.Ordered;
import org.springframework.web.server.WebFilter;

/**
 * An {@link Ordered} {@link org.springframework.web.server.WebFilter}.
 *
 * @author Phillip Webb
 * @since 4.0.0
 */
public interface OrderedWebFilter extends WebFilter, Ordered {

	/**
	 * Filters that wrap the request should be ordered less than or equal to this.
	 */
	int REQUEST_WRAPPER_FILTER_MAX_ORDER = 0;

}
