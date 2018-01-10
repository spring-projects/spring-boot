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

package org.springframework.boot.actuate.endpoint.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import org.springframework.boot.actuate.endpoint.Operation;
import org.springframework.boot.actuate.endpoint.OperationInvoker;
import org.springframework.boot.actuate.endpoint.OperationType;
import org.springframework.boot.actuate.endpoint.reflect.OperationMethodInfo;
import org.springframework.boot.actuate.endpoint.reflect.OperationMethodInvokerAdvisor;
import org.springframework.boot.actuate.endpoint.reflect.ParameterMapper;
import org.springframework.boot.actuate.endpoint.reflect.ReflectiveOperationInvoker;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.MethodIntrospector.MetadataLookup;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationAttributes;

/**
 * Factory to creates an {@link Operation} for a annotated methods on an
 * {@link Endpoint @Endpoint}.
 *
 * @param <T> The operation type
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @author Phillip Webb
 */
class OperationsFactory<T extends Operation> {

	private static final Map<OperationType, Class<? extends Annotation>> OPERATION_TYPES;

	static {
		Map<OperationType, Class<? extends Annotation>> operationTypes = new LinkedHashMap<>();
		operationTypes.put(OperationType.READ, ReadOperation.class);
		operationTypes.put(OperationType.WRITE, WriteOperation.class);
		operationTypes.put(OperationType.DELETE, DeleteOperation.class);
		OPERATION_TYPES = Collections.unmodifiableMap(operationTypes);
	}

	private final OperationFactory<T> operationFactory;

	private final ParameterMapper parameterMapper;

	private final Collection<OperationMethodInvokerAdvisor> invokerAdvisors;

	OperationsFactory(OperationFactory<T> operationFactory,
			ParameterMapper parameterMapper,
			Collection<? extends OperationMethodInvokerAdvisor> invokerAdvisors) {
		this.operationFactory = operationFactory;
		this.parameterMapper = parameterMapper;
		this.invokerAdvisors = (invokerAdvisors == null ? Collections.emptyList()
				: new ArrayList<>(invokerAdvisors));
	}

	public Map<Method, T> createOperations(String id, Object target, Class<?> type) {
		return MethodIntrospector.selectMethods(type,
				(MetadataLookup<T>) (method) -> createOperation(id, target, method));
	}

	private T createOperation(String endpointId, Object target, Method method) {
		return OPERATION_TYPES.entrySet().stream()
				.map((entry) -> createOperation(endpointId, target, method,
						entry.getKey(), entry.getValue()))
				.filter(Objects::nonNull).findFirst().orElse(null);
	}

	private T createOperation(String endpointId, Object target, Method method,
			OperationType operationType, Class<? extends Annotation> annotationType) {
		AnnotationAttributes annotationAttributes = AnnotatedElementUtils
				.getMergedAnnotationAttributes(method, annotationType);
		if (annotationAttributes == null) {
			return null;
		}
		OperationMethodInfo methodInfo = new OperationMethodInfo(method, operationType,
				annotationAttributes);
		OperationInvoker invoker = new ReflectiveOperationInvoker(target, methodInfo,
				this.parameterMapper);
		return this.operationFactory.createOperation(endpointId, methodInfo, target,
				applyAdvisors(endpointId, methodInfo, invoker));
	}

	private OperationInvoker applyAdvisors(String endpointId,
			OperationMethodInfo methodInfo, OperationInvoker invoker) {
		if (this.invokerAdvisors != null) {
			for (OperationMethodInvokerAdvisor advisor : this.invokerAdvisors) {
				invoker = advisor.apply(endpointId, methodInfo, invoker);
			}
		}
		return invoker;
	}

}
