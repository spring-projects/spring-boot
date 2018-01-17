/*
 * Copyright 2012-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.actuate.endpoint.reflect;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.boot.actuate.endpoint.OperationType;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.util.Assert;

/**
 * Information describing an operation method on an endpoint method.
 *
 * @author Phillip Webb
 * @since 2.0.0
 * @see ReflectiveOperationInvoker
 */
public final class OperationMethodInfo {

	private static final ParameterNameDiscoverer DEFAULT_PARAMETER_NAME_DISCOVERER = new DefaultParameterNameDiscoverer();

	private final Method method;

	private final OperationType operationType;

	private final AnnotationAttributes annotationAttributes;

	private final ParameterNameDiscoverer parameterNameDiscoverer = DEFAULT_PARAMETER_NAME_DISCOVERER;

	public OperationMethodInfo(Method method, OperationType operationType,
			AnnotationAttributes annotationAttributes) {
		Assert.notNull(method, "Method must not be null");
		Assert.notNull(operationType, "Operation Type must not be null");
		Assert.notNull(annotationAttributes, "Annotation Attributes must not be null");
		this.method = method;
		this.operationType = operationType;
		this.annotationAttributes = annotationAttributes;
	}

	/**
	 * Return the source Java method.
	 * @return the method
	 */
	public Method getMethod() {
		return this.method;
	}

	/**
	 * Return the operation type.
	 * @return the operation type
	 */
	public OperationType getOperationType() {
		return this.operationType;
	}

	/**
	 * Return the mime type that the operation produces.
	 * @return the produced mime type
	 */
	public String[] getProduces() {
		return this.annotationAttributes.getStringArray("produces");
	}

	/**
	 * Return a map of method parameters with the key being the discovered parameter name.
	 * @return the method parameters
	 */
	public Map<String, Parameter> getParameters() {
		Parameter[] parameters = this.method.getParameters();
		String[] names = this.parameterNameDiscoverer.getParameterNames(this.method);
		Assert.state(names != null,
				"Failed to extract parameter names for " + this.method);
		Map<String, Parameter> result = new LinkedHashMap<>();
		for (int i = 0; i < names.length; i++) {
			result.put(names[i], parameters[i]);
		}
		return result;
	}

}
