/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.actuate.endpoint.web.annotation;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Stream;

import org.springframework.boot.actuate.endpoint.OperationType;
import org.springframework.boot.actuate.endpoint.annotation.DiscoveredOperationMethod;
import org.springframework.boot.actuate.endpoint.annotation.Selector;
import org.springframework.boot.actuate.endpoint.annotation.Selector.Match;
import org.springframework.boot.actuate.endpoint.invoke.OperationParameter;
import org.springframework.boot.actuate.endpoint.web.EndpointMediaTypes;
import org.springframework.boot.actuate.endpoint.web.WebEndpointHttpMethod;
import org.springframework.boot.actuate.endpoint.web.WebEndpointResponse;
import org.springframework.boot.actuate.endpoint.web.WebOperationRequestPredicate;
import org.springframework.core.ResolvableType;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

/**
 * Factory to create a {@link WebOperationRequestPredicate}.
 *
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Moritz Halbritter
 */
class RequestPredicateFactory {

	private final EndpointMediaTypes endpointMediaTypes;

	/**
     * Constructs a new RequestPredicateFactory with the specified EndpointMediaTypes.
     * 
     * @param endpointMediaTypes the EndpointMediaTypes to be used by the RequestPredicateFactory (must not be null)
     * @throws IllegalArgumentException if the endpointMediaTypes parameter is null
     */
    RequestPredicateFactory(EndpointMediaTypes endpointMediaTypes) {
		Assert.notNull(endpointMediaTypes, "EndpointMediaTypes must not be null");
		this.endpointMediaTypes = endpointMediaTypes;
	}

	/**
     * Returns a {@link WebOperationRequestPredicate} based on the given root path and operation method.
     * 
     * @param rootPath the root path of the web operation
     * @param operationMethod the discovered operation method
     * @return the web operation request predicate
     */
    WebOperationRequestPredicate getRequestPredicate(String rootPath, DiscoveredOperationMethod operationMethod) {
		Method method = operationMethod.getMethod();
		OperationParameter[] selectorParameters = operationMethod.getParameters()
			.stream()
			.filter(this::hasSelector)
			.toArray(OperationParameter[]::new);
		OperationParameter allRemainingPathSegmentsParameter = getAllRemainingPathSegmentsParameter(selectorParameters);
		String path = getPath(rootPath, selectorParameters, allRemainingPathSegmentsParameter != null);
		WebEndpointHttpMethod httpMethod = determineHttpMethod(operationMethod.getOperationType());
		Collection<String> consumes = getConsumes(httpMethod, method);
		Collection<String> produces = getProduces(operationMethod, method);
		return new WebOperationRequestPredicate(path, httpMethod, consumes, produces);
	}

	/**
     * Retrieves the operation parameter that is annotated with the {@code @Selector} annotation
     * and has a match value of {@code Match.ALL_REMAINING}.
     * 
     * @param selectorParameters an array of operation parameters
     * @return the operation parameter with the {@code @Selector} annotation and match value of {@code Match.ALL_REMAINING},
     *         or {@code null} if no such parameter exists
     * @throws IllegalStateException if more than one operation parameter is annotated with {@code @Selector} and match value of {@code Match.ALL_REMAINING},
     *                               or if the parameter is not the last parameter in the array
     */
    private OperationParameter getAllRemainingPathSegmentsParameter(OperationParameter[] selectorParameters) {
		OperationParameter trailingPathsParameter = null;
		for (OperationParameter selectorParameter : selectorParameters) {
			Selector selector = selectorParameter.getAnnotation(Selector.class);
			if (selector.match() == Match.ALL_REMAINING) {
				Assert.state(trailingPathsParameter == null,
						"@Selector annotation with Match.ALL_REMAINING must be unique");
				trailingPathsParameter = selectorParameter;
			}
		}
		if (trailingPathsParameter != null) {
			Assert.state(trailingPathsParameter == selectorParameters[selectorParameters.length - 1],
					"@Selector annotation with Match.ALL_REMAINING must be the last parameter");
		}
		return trailingPathsParameter;
	}

	/**
     * Generates a path string based on the given root path, selector parameters, and matchRemainingPathSegments flag.
     * 
     * @param rootPath The root path to append the selector parameters to.
     * @param selectorParameters An array of OperationParameter objects representing the selector parameters.
     * @param matchRemainingPathSegments A boolean flag indicating whether to match remaining path segments.
     * @return The generated path string.
     */
    private String getPath(String rootPath, OperationParameter[] selectorParameters,
			boolean matchRemainingPathSegments) {
		StringBuilder path = new StringBuilder(rootPath);
		for (int i = 0; i < selectorParameters.length; i++) {
			path.append((i != 0 || !rootPath.endsWith("/")) ? "/{" : "{");
			if (i == selectorParameters.length - 1 && matchRemainingPathSegments) {
				path.append("*");
			}
			path.append(selectorParameters[i].getName());
			path.append("}");
		}
		return path.toString();
	}

	/**
     * Checks if the given OperationParameter has a Selector annotation.
     * 
     * @param parameter the OperationParameter to check
     * @return true if the OperationParameter has a Selector annotation, false otherwise
     */
    private boolean hasSelector(OperationParameter parameter) {
		return parameter.getAnnotation(Selector.class) != null;
	}

	/**
     * Returns the collection of media types that the given HTTP method and method consume, if the HTTP method is POST and the method consumes a request body.
     * 
     * @param httpMethod the HTTP method
     * @param method the method
     * @return the collection of media types that the given HTTP method and method consume, or an empty collection if the conditions are not met
     */
    private Collection<String> getConsumes(WebEndpointHttpMethod httpMethod, Method method) {
		if (WebEndpointHttpMethod.POST == httpMethod && consumesRequestBody(method)) {
			return this.endpointMediaTypes.getConsumed();
		}
		return Collections.emptyList();
	}

	/**
     * Returns the collection of media types that the given operation method produces.
     * 
     * @param operationMethod the discovered operation method
     * @param method the method object
     * @return the collection of media types that the operation method produces
     */
    private Collection<String> getProduces(DiscoveredOperationMethod operationMethod, Method method) {
		if (!operationMethod.getProducesMediaTypes().isEmpty()) {
			return operationMethod.getProducesMediaTypes();
		}
		if (Void.class.equals(method.getReturnType()) || void.class.equals(method.getReturnType())) {
			return Collections.emptyList();
		}
		if (producesResource(method)) {
			return Collections.singletonList("application/octet-stream");
		}
		return this.endpointMediaTypes.getProduced();
	}

	/**
     * Determines if the given method produces a resource.
     * 
     * @param method the method to check
     * @return true if the method produces a resource, false otherwise
     */
    private boolean producesResource(Method method) {
		if (Resource.class.equals(method.getReturnType())) {
			return true;
		}
		if (WebEndpointResponse.class.isAssignableFrom(method.getReturnType())) {
			ResolvableType returnType = ResolvableType.forMethodReturnType(method);
			return ResolvableType.forClass(Resource.class).isAssignableFrom(returnType.getGeneric(0));
		}
		return false;
	}

	/**
     * Determines if the given method consumes a request body.
     * 
     * @param method the method to check
     * @return true if the method consumes a request body, false otherwise
     */
    private boolean consumesRequestBody(Method method) {
		return Stream.of(method.getParameters())
			.anyMatch((parameter) -> parameter.getAnnotation(Selector.class) == null);
	}

	/**
     * Determines the HTTP method based on the given operation type.
     * 
     * @param operationType the operation type to determine the HTTP method for
     * @return the HTTP method to be used for the given operation type
     */
    private WebEndpointHttpMethod determineHttpMethod(OperationType operationType) {
		if (operationType == OperationType.WRITE) {
			return WebEndpointHttpMethod.POST;
		}
		if (operationType == OperationType.DELETE) {
			return WebEndpointHttpMethod.DELETE;
		}
		return WebEndpointHttpMethod.GET;
	}

}
