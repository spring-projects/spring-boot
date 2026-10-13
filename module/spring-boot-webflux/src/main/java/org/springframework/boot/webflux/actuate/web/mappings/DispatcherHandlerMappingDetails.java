/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.webflux.actuate.web.mappings;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.actuate.web.mappings.HandlerMethodDescription;
import org.springframework.web.reactive.DispatcherHandler;

/**
 * Details of a {@link DispatcherHandler} mapping.
 *
 * @author Andy Wilkinson
 * @since 4.0.0
 */
public class DispatcherHandlerMappingDetails {

	private @Nullable HandlerMethodDescription handlerMethod;

	private @Nullable HandlerFunctionDescription handlerFunction;

	private @Nullable RequestMappingConditionsDescription requestMappingConditions;

	public @Nullable HandlerMethodDescription getHandlerMethod() {
		return this.handlerMethod;
	}

	void setHandlerMethod(@Nullable HandlerMethodDescription handlerMethod) {
		this.handlerMethod = handlerMethod;
	}

	public @Nullable HandlerFunctionDescription getHandlerFunction() {
		return this.handlerFunction;
	}

	void setHandlerFunction(@Nullable HandlerFunctionDescription handlerFunction) {
		this.handlerFunction = handlerFunction;
	}

	public @Nullable RequestMappingConditionsDescription getRequestMappingConditions() {
		return this.requestMappingConditions;
	}

	void setRequestMappingConditions(@Nullable RequestMappingConditionsDescription requestMappingConditions) {
		this.requestMappingConditions = requestMappingConditions;
	}

}
