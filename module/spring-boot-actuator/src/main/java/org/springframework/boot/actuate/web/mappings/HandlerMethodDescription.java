/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.web.mappings;

import org.springframework.asm.Type;
import org.springframework.web.method.HandlerMethod;

/**
 * A description of a {@link HandlerMethod}.
 *
 * @author Andy Wilkinson
 * @since 2.0.0
 */
public class HandlerMethodDescription {

	private final String className;

	private final String name;

	private final String descriptor;

	public HandlerMethodDescription(HandlerMethod handlerMethod) {
		this.name = handlerMethod.getMethod().getName();
		this.className = handlerMethod.getMethod().getDeclaringClass().getCanonicalName();
		this.descriptor = Type.getMethodDescriptor(handlerMethod.getMethod());
	}

	public String getName() {
		return this.name;
	}

	public String getDescriptor() {
		return this.descriptor;
	}

	public String getClassName() {
		return this.className;
	}

}
