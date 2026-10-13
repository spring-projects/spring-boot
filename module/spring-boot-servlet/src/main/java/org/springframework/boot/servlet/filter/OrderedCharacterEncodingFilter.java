/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.servlet.filter;

import org.springframework.core.Ordered;
import org.springframework.web.filter.CharacterEncodingFilter;

/**
 * {@link CharacterEncodingFilter} that also implements {@link Ordered}.
 *
 * @author Phillip Webb
 * @since 4.0.0
 */
public class OrderedCharacterEncodingFilter extends CharacterEncodingFilter implements OrderedFilter {

	private int order = Ordered.HIGHEST_PRECEDENCE;

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
