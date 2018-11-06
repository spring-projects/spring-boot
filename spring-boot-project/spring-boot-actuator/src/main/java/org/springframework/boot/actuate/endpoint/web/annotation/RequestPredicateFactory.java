/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.actuate.endpoint.web.annotation;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.boot.actuate.endpoint.EndpointId;
import org.springframework.boot.actuate.endpoint.OperationType;
import org.springframework.boot.actuate.endpoint.annotation.DiscoveredOperationMethod;
import org.springframework.boot.actuate.endpoint.annotation.Selector;
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
 */
class RequestPredicateFactory {

	private final EndpointMediaTypes endpointMediaTypes;

	RequestPredicateFactory(EndpointMediaTypes endpointMediaTypes) {
		Assert.notNull(endpointMediaTypes, "EndpointMediaTypes must not be null");
		this.endpointMediaTypes = endpointMediaTypes;
	}

	public WebOperationRequestPredicate getRequestPredicate(EndpointId endpointId,
			String rootPath, DiscoveredOperationMethod operationMethod) {
		Method method = operationMethod.getMethod();
		String path = getPath(rootPath, method);
		WebEndpointHttpMethod httpMethod = determineHttpMethod(
				operationMethod.getOperationType());
		Collection<String> consumes = getConsumes(httpMethod, method);
		Collection<String> produces = getProduces(operationMethod, method);
		return new WebOperationRequestPredicate(path, httpMethod, consumes, produces);
	}

	private String getPath(String rootPath, Method method) {
		return rootPath + Stream.of(method.getParameters()).filter(this::hasSelector)
				.map(this::slashName).collect(Collectors.joining());
	}

	private boolean hasSelector(Parameter parameter) {
		return parameter.getAnnotation(Selector.class) != null;
	}

	private String slashName(Parameter parameter) {
		return "/{" + parameter.getName() + "}";
	}

	private Collection<String> getConsumes(WebEndpointHttpMethod httpMethod,
			Method method) {
		if (WebEndpointHttpMethod.POST == httpMethod && consumesRequestBody(method)) {
			return this.endpointMediaTypes.getConsumed();
		}
		return Collections.emptyList();
	}

	private Collection<String> getProduces(DiscoveredOperationMethod operationMethod,
			Method method) {
		if (!operationMethod.getProducesMediaTypes().isEmpty()) {
			return operationMethod.getProducesMediaTypes();
		}
		if (Void.class.equals(method.getReturnType())
				|| void.class.equals(method.getReturnType())) {
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
			if (ResolvableType.forClass(Resource.class)
					.isAssignableFrom(returnType.getGeneric(0))) {
				return true;
			}
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
