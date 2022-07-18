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

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

import org.springframework.aot.hint.ExecutableMode;
import org.springframework.aot.hint.ReflectionHints;
import org.springframework.aot.hint.annotation.ReflectiveProcessor;
import org.springframework.boot.actuate.endpoint.web.WebEndpointResponse;
import org.springframework.context.aot.BindingReflectionHintsRegistrar;
import org.springframework.core.ResolvableType;
import org.springframework.core.io.Resource;

/**
 * Processor which registers the annotated operation method and its return type for
 * reflection.
 *
 * @author Moritz Halbritter
 */
class OperationReflectiveProcessor implements ReflectiveProcessor {

	private final BindingReflectionHintsRegistrar bindingRegistrar = new BindingReflectionHintsRegistrar();

	@Override
	public void registerReflectionHints(ReflectionHints hints, AnnotatedElement element) {
		if (!(element instanceof Method method)) {
			throw new IllegalArgumentException("This processor can only be invoked for annotated methods");
		}
		hints.registerMethod(method, (hint) -> hint.setModes(ExecutableMode.INVOKE));
		registerReturnValueHints(hints, method);
	}

	private void registerReturnValueHints(ReflectionHints hints, Method method) {
		ResolvableType returnType = ResolvableType.forMethodReturnType(method);
		if (WebEndpointResponse.class.isAssignableFrom(method.getReturnType())) {
			registerWebEndpointResponse(hints, returnType);
		}
		else {
			registerReflectionHints(hints, returnType.getType());
		}
	}

	private void registerWebEndpointResponse(ReflectionHints hints, ResolvableType returnType) {
		ResolvableType genericParameter = returnType.getGeneric(0);
		if (genericParameter.getRawClass() != null) {
			registerReflectionHints(hints, genericParameter.getType());
		}
	}

	private void registerReflectionHints(ReflectionHints hints, Type type) {
		if (type.equals(Resource.class)) {
			return;
		}
		this.bindingRegistrar.registerReflectionHints(hints, type);
	}

}
