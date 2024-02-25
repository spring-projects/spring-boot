/*
 * Copyright 2012-2023 the original author or authors.
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

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

import org.springframework.boot.actuate.endpoint.EndpointId;
import org.springframework.boot.actuate.endpoint.Operation;
import org.springframework.boot.actuate.endpoint.OperationType;
import org.springframework.boot.actuate.endpoint.invoke.OperationInvoker;
import org.springframework.boot.actuate.endpoint.invoke.OperationInvokerAdvisor;
import org.springframework.boot.actuate.endpoint.invoke.ParameterValueMapper;
import org.springframework.boot.actuate.endpoint.invoke.reflect.OperationMethod;
import org.springframework.boot.actuate.endpoint.invoke.reflect.ReflectiveOperationInvoker;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.MethodIntrospector.MetadataLookup;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;

/**
 * Factory to create an {@link Operation} for annotated methods on an
 * {@link Endpoint @Endpoint} or {@link EndpointExtension @EndpointExtension}.
 *
 * @param <O> the operation type
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @author Phillip Webb
 */
abstract class DiscoveredOperationsFactory<O extends Operation> {

	private static final Map<OperationType, Class<? extends Annotation>> OPERATION_TYPES;

	static {
		Map<OperationType, Class<? extends Annotation>> operationTypes = new EnumMap<>(OperationType.class);
		operationTypes.put(OperationType.READ, ReadOperation.class);
		operationTypes.put(OperationType.WRITE, WriteOperation.class);
		operationTypes.put(OperationType.DELETE, DeleteOperation.class);
		OPERATION_TYPES = Collections.unmodifiableMap(operationTypes);
	}

	private final ParameterValueMapper parameterValueMapper;

	private final Collection<OperationInvokerAdvisor> invokerAdvisors;

	/**
     * Constructs a new DiscoveredOperationsFactory with the specified ParameterValueMapper and
     * collection of OperationInvokerAdvisors.
     * 
     * @param parameterValueMapper the ParameterValueMapper used for mapping parameter values
     * @param invokerAdvisors the collection of OperationInvokerAdvisors used for advising operation invokers
     */
    DiscoveredOperationsFactory(ParameterValueMapper parameterValueMapper,
			Collection<OperationInvokerAdvisor> invokerAdvisors) {
		this.parameterValueMapper = parameterValueMapper;
		this.invokerAdvisors = invokerAdvisors;
	}

	/**
     * Creates a collection of operations for the given endpoint ID and target object.
     * 
     * @param id the ID of the endpoint
     * @param target the target object
     * @return a collection of operations
     */
    Collection<O> createOperations(EndpointId id, Object target) {
		return MethodIntrospector
			.selectMethods(target.getClass(), (MetadataLookup<O>) (method) -> createOperation(id, target, method))
			.values();
	}

	/**
     * Creates an operation based on the given endpoint ID, target object, and method.
     * 
     * @param endpointId the ID of the endpoint
     * @param target the target object on which the method will be invoked
     * @param method the method to be invoked
     * @return the created operation, or null if no operation could be created
     */
    private O createOperation(EndpointId endpointId, Object target, Method method) {
		return OPERATION_TYPES.entrySet()
			.stream()
			.map((entry) -> createOperation(endpointId, target, method, entry.getKey(), entry.getValue()))
			.filter(Objects::nonNull)
			.findFirst()
			.orElse(null);
	}

	/**
     * Creates an operation based on the provided parameters.
     * 
     * @param endpointId the ID of the endpoint
     * @param target the target object on which the operation will be invoked
     * @param method the method representing the operation
     * @param operationType the type of the operation
     * @param annotationType the type of annotation associated with the operation
     * @return the created operation, or null if the annotation is not present
     */
    private O createOperation(EndpointId endpointId, Object target, Method method, OperationType operationType,
			Class<? extends Annotation> annotationType) {
		MergedAnnotation<?> annotation = MergedAnnotations.from(method).get(annotationType);
		if (!annotation.isPresent()) {
			return null;
		}
		DiscoveredOperationMethod operationMethod = new DiscoveredOperationMethod(method, operationType,
				annotation.asAnnotationAttributes());
		OperationInvoker invoker = new ReflectiveOperationInvoker(target, operationMethod, this.parameterValueMapper);
		invoker = applyAdvisors(endpointId, operationMethod, invoker);
		return createOperation(endpointId, operationMethod, invoker);
	}

	/**
     * Applies advisors to the given operation invoker.
     * 
     * @param endpointId the endpoint ID
     * @param operationMethod the operation method
     * @param invoker the operation invoker
     * @return the operation invoker with advisors applied
     */
    private OperationInvoker applyAdvisors(EndpointId endpointId, OperationMethod operationMethod,
			OperationInvoker invoker) {
		if (this.invokerAdvisors != null) {
			for (OperationInvokerAdvisor advisor : this.invokerAdvisors) {
				invoker = advisor.apply(endpointId, operationMethod.getOperationType(), operationMethod.getParameters(),
						invoker);
			}
		}
		return invoker;
	}

	/**
     * Creates a new operation with the specified endpoint ID, operation method, and invoker.
     *
     * @param endpointId      the ID of the endpoint
     * @param operationMethod the discovered operation method
     * @param invoker         the operation invoker
     * @return the created operation
     */
    protected abstract O createOperation(EndpointId endpointId, DiscoveredOperationMethod operationMethod,
			OperationInvoker invoker);

}
