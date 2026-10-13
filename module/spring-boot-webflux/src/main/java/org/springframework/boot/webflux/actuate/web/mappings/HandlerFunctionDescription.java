/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.webflux.actuate.web.mappings;

import org.springframework.web.reactive.function.server.HandlerFunction;

/**
 * Description of a {@link HandlerFunction}.
 *
 * @author Andy Wilkinson
 * @since 4.0.0
 */
public class HandlerFunctionDescription {

	private final String className;

	HandlerFunctionDescription(HandlerFunction<?> handlerFunction) {
		this.className = getHandlerFunctionClassName(handlerFunction);
	}

	private static String getHandlerFunctionClassName(HandlerFunction<?> handlerFunction) {
		Class<?> functionClass = handlerFunction.getClass();
		String canonicalName = functionClass.getCanonicalName();
		return (canonicalName != null) ? canonicalName : functionClass.getName();
	}

	public String getClassName() {
		return this.className;
	}

}
