/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.endpoint.web;

import org.springframework.core.style.ToStringCreator;
import org.springframework.util.Assert;

/**
 * Details for a link in a
 * <a href="https://tools.ietf.org/html/draft-kelly-json-hal-08">HAL</a>-formatted
 * response.
 *
 * @author Andy Wilkinson
 * @since 2.0.0
 */
public class Link {

	private final String href;

	private final boolean templated;

	/**
	 * Creates a new {@link Link} with the given {@code href}.
	 * @param href the href
	 */
	public Link(String href) {
		Assert.notNull(href, "'href' must not be null");
		this.href = href;
		this.templated = href.contains("{");
	}

	/**
	 * Returns the href of the link.
	 * @return the href
	 */
	public String getHref() {
		return this.href;
	}

	/**
	 * Returns whether the {@link #getHref() href} is templated.
	 * @return {@code true} if the href is templated, otherwise {@code false}
	 */
	public boolean isTemplated() {
		return this.templated;
	}

	@Override
	public String toString() {
		return new ToStringCreator(this).append("href", this.href).toString();
	}

}
