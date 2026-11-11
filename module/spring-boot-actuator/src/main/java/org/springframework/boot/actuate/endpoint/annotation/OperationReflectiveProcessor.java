/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.endpoint.annotation;

import java.lang.reflect.Method;
import java.lang.reflect.Type;

import org.springframework.aot.hint.BindingReflectionHintsRegistrar;
import org.springframework.aot.hint.ReflectionHints;
import org.springframework.aot.hint.annotation.ReflectiveProcessor;
import org.springframework.aot.hint.annotation.SimpleReflectiveProcessor;
import org.springframework.boot.actuate.endpoint.web.WebEndpointResponse;
import org.springframework.core.ResolvableType;
import org.springframework.core.io.Resource;

/**
 * {@link ReflectiveProcessor} that registers the annotated operation method and its
 * return type for reflection.
 *
 * @author Moritz Halbritter
 * @author Stephane Nicoll
 */
class OperationReflectiveProcessor extends SimpleReflectiveProcessor {

	private final BindingReflectionHintsRegistrar bindingRegistrar = new BindingReflectionHintsRegistrar();

	@Override
	protected void registerMethodHint(ReflectionHints hints, Method method) {
		super.registerMethodHint(hints, method);
		Type returnType = extractReturnType(method);
		if (returnType != null) {
			registerReflectionHints(hints, returnType);
		}
	}

	private Type extractReturnType(Method method) {
		ResolvableType returnType = ResolvableType.forMethodReturnType(method);
		if (!WebEndpointResponse.class.isAssignableFrom(method.getReturnType())) {
			return returnType.getType();
		}
		return returnType.as(WebEndpointResponse.class).getGeneric(0).getType();
	}

	private void registerReflectionHints(ReflectionHints hints, Type type) {
		if (!type.equals(Resource.class)) {
			this.bindingRegistrar.registerReflectionHints(hints, type);
		}
	}

}
