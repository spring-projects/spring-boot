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

package org.springframework.boot.actuate.endpoint.web.annotation;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.reactivestreams.Publisher;

import org.springframework.boot.actuate.endpoint.EndpointExposure;
import org.springframework.boot.actuate.endpoint.EndpointInfo;
import org.springframework.boot.actuate.endpoint.OperationInvoker;
import org.springframework.boot.actuate.endpoint.OperationParameterMapper;
import org.springframework.boot.actuate.endpoint.OperationType;
import org.springframework.boot.actuate.endpoint.ReflectiveOperationInvoker;
import org.springframework.boot.actuate.endpoint.annotation.AnnotationEndpointDiscoverer;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.Selector;
import org.springframework.boot.actuate.endpoint.cache.CachingConfiguration;
import org.springframework.boot.actuate.endpoint.cache.CachingConfigurationFactory;
import org.springframework.boot.actuate.endpoint.cache.CachingOperationInvoker;
import org.springframework.boot.actuate.endpoint.web.OperationRequestPredicate;
import org.springframework.boot.actuate.endpoint.web.WebEndpointHttpMethod;
import org.springframework.boot.actuate.endpoint.web.WebEndpointOperation;
import org.springframework.boot.actuate.endpoint.web.WebEndpointResponse;
import org.springframework.context.ApplicationContext;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.io.Resource;
import org.springframework.util.ClassUtils;

/**
 * Discovers the {@link Endpoint endpoints} in an {@link ApplicationContext} with
 * {@link WebEndpointExtension web extensions} applied to them.
 *
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @since 2.0.0
 */
public class WebAnnotationEndpointDiscoverer extends
		AnnotationEndpointDiscoverer<WebEndpointOperation, OperationRequestPredicate> {

	/**
	 * Creates a new {@link WebAnnotationEndpointDiscoverer} that will discover
	 * {@link Endpoint endpoints} and {@link WebEndpointExtension web extensions} using
	 * the given {@link ApplicationContext}.
	 * @param applicationContext the application context
	 * @param operationParameterMapper the {@link OperationParameterMapper} used to
	 * convert arguments when an operation is invoked
	 * @param cachingConfigurationFactory the {@link CachingConfiguration} factory to use
	 * @param consumedMediaTypes the media types consumed by web endpoint operations
	 * @param producedMediaTypes the media types produced by web endpoint operations
	 */
	public WebAnnotationEndpointDiscoverer(ApplicationContext applicationContext,
			OperationParameterMapper operationParameterMapper,
			CachingConfigurationFactory cachingConfigurationFactory,
			Collection<String> consumedMediaTypes,
			Collection<String> producedMediaTypes) {
		super(applicationContext,
				new WebEndpointOperationFactory(operationParameterMapper,
						consumedMediaTypes, producedMediaTypes),
				WebEndpointOperation::getRequestPredicate, cachingConfigurationFactory);
	}

	@Override
	public Collection<EndpointInfo<WebEndpointOperation>> discoverEndpoints() {
		Collection<EndpointInfoDescriptor<WebEndpointOperation, OperationRequestPredicate>> endpoints = discoverEndpoints(
				WebEndpointExtension.class, EndpointExposure.WEB);
		verifyThatOperationsHaveDistinctPredicates(endpoints);
		return endpoints.stream().map(EndpointInfoDescriptor::getEndpointInfo)
				.collect(Collectors.toList());
	}

	private void verifyThatOperationsHaveDistinctPredicates(
			Collection<EndpointInfoDescriptor<WebEndpointOperation, OperationRequestPredicate>> endpointDescriptors) {
		List<List<WebEndpointOperation>> clashes = new ArrayList<>();
		endpointDescriptors.forEach((descriptor) -> clashes
				.addAll(descriptor.findDuplicateOperations().values()));
		if (!clashes.isEmpty()) {
			StringBuilder message = new StringBuilder();
			message.append(String.format(
					"Found multiple web operations with matching request predicates:%n"));
			clashes.forEach((clash) -> {
				message.append("    ").append(clash.get(0).getRequestPredicate())
						.append(String.format(":%n"));
				clash.forEach((operation) -> message.append("        ")
						.append(String.format("%s%n", operation)));
			});
			throw new IllegalStateException(message.toString());
		}
	}

	private static final class WebEndpointOperationFactory
			implements EndpointOperationFactory<WebEndpointOperation> {

		private static final boolean REACTIVE_STREAMS_PRESENT = ClassUtils.isPresent(
				"org.reactivestreams.Publisher",
				WebEndpointOperationFactory.class.getClassLoader());

		private final OperationParameterMapper parameterMapper;

		private final Collection<String> consumedMediaTypes;

		private final Collection<String> producedMediaTypes;

		private WebEndpointOperationFactory(OperationParameterMapper parameterMapper,
				Collection<String> consumedMediaTypes,
				Collection<String> producedMediaTypes) {
			this.parameterMapper = parameterMapper;
			this.consumedMediaTypes = consumedMediaTypes;
			this.producedMediaTypes = producedMediaTypes;
		}

		@Override
		public WebEndpointOperation createOperation(String endpointId,
				AnnotationAttributes operationAttributes, Object target, Method method,
				OperationType type, long timeToLive) {
			WebEndpointHttpMethod httpMethod = determineHttpMethod(type);
			OperationRequestPredicate requestPredicate = new OperationRequestPredicate(
					determinePath(endpointId, method), httpMethod,
					determineConsumedMediaTypes(httpMethod, method),
					determineProducedMediaTypes(
							operationAttributes.getStringArray("produces"), method));
			OperationInvoker invoker = new ReflectiveOperationInvoker(
					this.parameterMapper, target, method);
			if (timeToLive > 0) {
				invoker = new CachingOperationInvoker(invoker, timeToLive);
			}
			return new WebEndpointOperation(type, invoker, determineBlocking(method),
					requestPredicate, determineId(endpointId, method));
		}

		private String determinePath(String endpointId, Method operationMethod) {
			StringBuilder path = new StringBuilder(endpointId);
			Stream.of(operationMethod.getParameters())
					.filter((
							parameter) -> parameter.getAnnotation(Selector.class) != null)
					.map((parameter) -> "/{" + parameter.getName() + "}")
					.forEach(path::append);
			return path.toString();
		}

		private String determineId(String endpointId, Method operationMethod) {
			StringBuilder path = new StringBuilder(endpointId);
			Stream.of(operationMethod.getParameters())
					.filter((
							parameter) -> parameter.getAnnotation(Selector.class) != null)
					.map((parameter) -> "-" + parameter.getName()).forEach(path::append);
			return path.toString();
		}

		private Collection<String> determineConsumedMediaTypes(
				WebEndpointHttpMethod httpMethod, Method method) {
			if (WebEndpointHttpMethod.POST == httpMethod && consumesRequestBody(method)) {
				return this.consumedMediaTypes;
			}
			return Collections.emptyList();
		}

		private Collection<String> determineProducedMediaTypes(String[] produces,
				Method method) {
			if (produces.length > 0) {
				return Arrays.asList(produces);
			}
			if (Void.class.equals(method.getReturnType())
					|| void.class.equals(method.getReturnType())) {
				return Collections.emptyList();
			}
			if (producesResourceResponseBody(method)) {
				return Collections.singletonList("application/octet-stream");
			}
			return this.producedMediaTypes;
		}

		private boolean producesResourceResponseBody(Method method) {
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
			return Stream.of(method.getParameters()).anyMatch(
					(parameter) -> parameter.getAnnotation(Selector.class) == null);
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

		private boolean determineBlocking(Method method) {
			return !REACTIVE_STREAMS_PRESENT
					|| !Publisher.class.isAssignableFrom(method.getReturnType());
		}

	}

}
