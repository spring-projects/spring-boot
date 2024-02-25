/*
 * Copyright 2012-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

	/**
	 * Registers a method hint with the given reflection hints and method.
	 * @param hints the reflection hints to register with
	 * @param method the method to register
	 */
	@Override
	protected void registerMethodHint(ReflectionHints hints, Method method) {
		super.registerMethodHint(hints, method);
		Type returnType = extractReturnType(method);
		if (returnType != null) {
			registerReflectionHints(hints, returnType);
		}
	}

	/**
	 * Extracts the return type of a given method.
	 * @param method the method to extract the return type from
	 * @return the extracted return type
	 */
	private Type extractReturnType(Method method) {
		ResolvableType returnType = ResolvableType.forMethodReturnType(method);
		if (!WebEndpointResponse.class.isAssignableFrom(method.getReturnType())) {
			return returnType.getType();
		}
		return returnType.as(WebEndpointResponse.class).getGeneric(0).getType();
	}

	/**
	 * Registers reflection hints for a given type.
	 * @param hints the reflection hints to register
	 * @param type the type for which the reflection hints are registered
	 */
	private void registerReflectionHints(ReflectionHints hints, Type type) {
		if (!type.equals(Resource.class)) {
			this.bindingRegistrar.registerReflectionHints(hints, type);
		}
	}

}
