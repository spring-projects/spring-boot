/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.webflux.actuate.web.mappings;

import org.jspecify.annotations.Nullable;

import org.springframework.web.reactive.DispatcherHandler;

/**
 * A description of a mapping known to a {@link DispatcherHandler}.
 *
 * @author Andy Wilkinson
 * @since 4.0.0
 */
public class DispatcherHandlerMappingDescription {

	private final String predicate;

	private final String handler;

	private final @Nullable DispatcherHandlerMappingDetails details;

	DispatcherHandlerMappingDescription(String predicate, String handler,
			@Nullable DispatcherHandlerMappingDetails details) {
		this.predicate = predicate;
		this.handler = handler;
		this.details = details;
	}

	public String getHandler() {
		return this.handler;
	}

	public String getPredicate() {
		return this.predicate;
	}

	public @Nullable DispatcherHandlerMappingDetails getDetails() {
		return this.details;
	}

}
