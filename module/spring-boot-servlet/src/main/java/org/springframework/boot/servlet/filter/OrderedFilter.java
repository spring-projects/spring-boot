/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.servlet.filter;

import jakarta.servlet.Filter;

import org.springframework.core.Ordered;

/**
 * An {@link Ordered} {@link jakarta.servlet.Filter}.
 *
 * @author Phillip Webb
 * @since 4.0.0
 */
public interface OrderedFilter extends Filter, Ordered {

	/**
	 * Filters that wrap the servlet request should be ordered less than or equal to this.
	 */
	int REQUEST_WRAPPER_FILTER_MAX_ORDER = 0;

}
