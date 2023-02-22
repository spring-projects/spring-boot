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

	RequestPredicateFactory(EndpointMediaTypes endpointMediaTypes) {
		Assert.notNull(endpointMediaTypes, "EndpointMediaTypes must not be null");
		this.endpointMediaTypes = endpointMediaTypes;
	}

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

	private String getPath(String rootPath, OperationParameter[] selectorParameters,
			boolean matchRemainingPathSegments) {
		StringBuilder path = new StringBuilder(rootPath);
		for (int i = 0; i < selectorParameters.length; i++) {
			path.append("/{");
			if (i == selectorParameters.length - 1 && matchRemainingPathSegments) {
				path.append("*");
			}
			path.append(selectorParameters[i].getName());
			path.append("}");
		}
		return path.toString();
	}

	private boolean hasSelector(OperationParameter parameter) {
		return parameter.getAnnotation(Selector.class) != null;
	}

	private Collection<String> getConsumes(WebEndpointHttpMethod httpMethod, Method method) {
		if (WebEndpointHttpMethod.POST == httpMethod && consumesRequestBody(method)) {
			return this.endpointMediaTypes.getConsumed();
		}
		return Collections.emptyList();
	}

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

	private boolean consumesRequestBody(Method method) {
		return Stream.of(method.getParameters())
			.anyMatch((parameter) -> parameter.getAnnotation(Selector.class) == null);
	}

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
