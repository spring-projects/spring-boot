/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.webmvc.actuate.web.mappings;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.actuate.web.mappings.HandlerMethodDescription;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * Details of a {@link DispatcherServlet} mapping.
 *
 * @author Andy Wilkinson
 * @author Xiong Tang
 * @since 4.0.0
 */
public class DispatcherServletMappingDetails {

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
