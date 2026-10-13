/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.servlet.filter;

import org.springframework.core.Ordered;
import org.springframework.web.filter.FormContentFilter;

/**
 * {@link FormContentFilter} that also implements {@link Ordered}.
 *
 * @author Joao Pedro Evangelista
 * @author Brian Clozel
 * @since 4.0.0
 */
public class OrderedFormContentFilter extends FormContentFilter implements OrderedFilter {

	/**
	 * Higher order to ensure the filter is applied before Spring Security.
	 */
	public static final int DEFAULT_ORDER = REQUEST_WRAPPER_FILTER_MAX_ORDER - 9900;

	private int order = DEFAULT_ORDER;

	@Override
	public int getOrder() {
		return this.order;
	}

	/**
	 * Set the order for this filter.
	 * @param order the order to set
	 */
	public void setOrder(int order) {
		this.order = order;
	}

}
