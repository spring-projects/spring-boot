/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.webmvc.actuate.web.mappings;

import org.jspecify.annotations.Nullable;

import org.springframework.web.servlet.DispatcherServlet;

/**
 * A description of a mapping known to a {@link DispatcherServlet}.
 *
 * @author Andy Wilkinson
 * @since 4.0.0
 */
public class DispatcherServletMappingDescription {

	private final String handler;

	private final String predicate;

	private final @Nullable DispatcherServletMappingDetails details;

	DispatcherServletMappingDescription(String predicate, String handler,
			@Nullable DispatcherServletMappingDetails details) {
		this.handler = handler;
		this.predicate = predicate;
		this.details = details;
	}

	public String getHandler() {
		return this.handler;
	}

	public String getPredicate() {
		return this.predicate;
	}

	public @Nullable DispatcherServletMappingDetails getDetails() {
		return this.details;
	}

}
