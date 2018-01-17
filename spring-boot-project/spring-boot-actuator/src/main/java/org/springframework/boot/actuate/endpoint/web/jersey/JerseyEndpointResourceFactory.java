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

package org.springframework.boot.actuate.endpoint.web.jersey;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.glassfish.jersey.process.Inflector;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.model.Resource;
import org.glassfish.jersey.server.model.Resource.Builder;
import reactor.core.publisher.Mono;

import org.springframework.boot.actuate.endpoint.EndpointInfo;
import org.springframework.boot.actuate.endpoint.OperationInvoker;
import org.springframework.boot.actuate.endpoint.reflect.ParameterMappingException;
import org.springframework.boot.actuate.endpoint.reflect.ParametersMissingException;
import org.springframework.boot.actuate.endpoint.web.EndpointLinksResolver;
import org.springframework.boot.actuate.endpoint.web.EndpointMediaTypes;
import org.springframework.boot.actuate.endpoint.web.Link;
import org.springframework.boot.actuate.endpoint.web.OperationRequestPredicate;
import org.springframework.boot.actuate.endpoint.web.WebEndpointResponse;
import org.springframework.boot.actuate.endpoint.web.WebOperation;
import org.springframework.boot.endpoint.web.EndpointMapping;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * A factory for creating Jersey {@link Resource Resources} for {@link WebOperation web
 * endpoint operations}.
 *
 * @author Andy Wilkinson
 * @since 2.0.0
 */
public class JerseyEndpointResourceFactory {

	private final EndpointLinksResolver endpointLinksResolver = new EndpointLinksResolver();

	/**
	 * Creates {@link Resource Resources} for the operations of the given
	 * {@code webEndpoints}.
	 * @param endpointMapping the base mapping for all endpoints
	 * @param webEndpoints the web endpoints
	 * @param endpointMediaTypes media types consumed and produced by the endpoints
	 * @return the resources for the operations
	 */
	public Collection<Resource> createEndpointResources(EndpointMapping endpointMapping,
			Collection<EndpointInfo<WebOperation>> webEndpoints,
			EndpointMediaTypes endpointMediaTypes) {
		List<Resource> resources = new ArrayList<>();
		webEndpoints.stream()
				.flatMap((endpointInfo) -> endpointInfo.getOperations().stream())
				.map((operation) -> createResource(endpointMapping, operation))
				.forEach(resources::add);
		if (StringUtils.hasText(endpointMapping.getPath())) {
			resources.add(createEndpointLinksResource(endpointMapping.getPath(),
					webEndpoints, endpointMediaTypes));
		}
		return resources;
	}

	private Resource createResource(EndpointMapping endpointMapping,
			WebOperation operation) {
		OperationRequestPredicate requestPredicate = operation.getRequestPredicate();
		Builder resourceBuilder = Resource.builder()
				.path(endpointMapping.createSubPath(requestPredicate.getPath()));
		resourceBuilder.addMethod(requestPredicate.getHttpMethod().name())
				.consumes(toStringArray(requestPredicate.getConsumes()))
				.produces(toStringArray(requestPredicate.getProduces()))
				.handledBy(new EndpointInvokingInflector(operation.getInvoker(),
						!requestPredicate.getConsumes().isEmpty()));
		return resourceBuilder.build();
	}

	private String[] toStringArray(Collection<String> collection) {
		return collection.toArray(new String[collection.size()]);
	}

	private Resource createEndpointLinksResource(String endpointPath,
			Collection<EndpointInfo<WebOperation>> webEndpoints,
			EndpointMediaTypes endpointMediaTypes) {
		Builder resourceBuilder = Resource.builder().path(endpointPath);
		resourceBuilder.addMethod("GET")
				.produces(endpointMediaTypes.getProduced()
						.toArray(new String[endpointMediaTypes.getProduced().size()]))
				.handledBy(new EndpointLinksInflector(webEndpoints,
						this.endpointLinksResolver));
		return resourceBuilder.build();
	}

	/**
	 * {@link Inflector} to invoke the endpoint.
	 */
	private static final class EndpointInvokingInflector
			implements Inflector<ContainerRequestContext, Object> {

		private static final List<Function<Object, Object>> BODY_CONVERTERS;

		static {
			List<Function<Object, Object>> converters = new ArrayList<>();
			converters.add(new ResourceBodyConverter());
			if (ClassUtils.isPresent("reactor.core.publisher.Mono",
					EndpointInvokingInflector.class.getClassLoader())) {
				converters.add(new MonoBodyConverter());
			}
			BODY_CONVERTERS = Collections.unmodifiableList(converters);
		}

		private final OperationInvoker operationInvoker;

		private final boolean readBody;

		private EndpointInvokingInflector(OperationInvoker operationInvoker,
				boolean readBody) {
			this.operationInvoker = operationInvoker;
			this.readBody = readBody;
		}

		@Override
		public Response apply(ContainerRequestContext data) {
			Map<String, Object> arguments = new HashMap<>();
			if (this.readBody) {
				arguments.putAll(extractBodyArguments(data));
			}
			arguments.putAll(extractPathParameters(data));
			arguments.putAll(extractQueryParameters(data));
			try {
				Object response = this.operationInvoker.invoke(arguments);
				return convertToJaxRsResponse(response, data.getRequest().getMethod());
			}
			catch (ParametersMissingException | ParameterMappingException ex) {
				return Response.status(Status.BAD_REQUEST).build();
			}
		}

		@SuppressWarnings("unchecked")
		private Map<String, Object> extractBodyArguments(ContainerRequestContext data) {
			Map<?, ?> entity = ((ContainerRequest) data).readEntity(Map.class);
			if (entity == null) {
				return Collections.emptyMap();
			}
			return (Map<String, Object>) entity;
		}

		private Map<String, Object> extractPathParameters(
				ContainerRequestContext requestContext) {
			return extract(requestContext.getUriInfo().getPathParameters());
		}

		private Map<String, Object> extractQueryParameters(
				ContainerRequestContext requestContext) {
			return extract(requestContext.getUriInfo().getQueryParameters());
		}

		private Map<String, Object> extract(
				MultivaluedMap<String, String> multivaluedMap) {
			Map<String, Object> result = new HashMap<>();
			multivaluedMap.forEach((name, values) -> {
				if (!CollectionUtils.isEmpty(values)) {
					result.put(name, values.size() == 1 ? values.get(0) : values);
				}
			});
			return result;
		}

		private Response convertToJaxRsResponse(Object response, String httpMethod) {
			if (response == null) {
				boolean isGet = HttpMethod.GET.equals(httpMethod);
				Status status = (isGet ? Status.NOT_FOUND : Status.NO_CONTENT);
				return Response.status(status).build();
			}
			try {
				if (!(response instanceof WebEndpointResponse)) {
					return Response.status(Status.OK).entity(convertIfNecessary(response))
							.build();
				}
				WebEndpointResponse<?> webEndpointResponse = (WebEndpointResponse<?>) response;
				return Response.status(webEndpointResponse.getStatus())
						.entity(convertIfNecessary(webEndpointResponse.getBody()))
						.build();
			}
			catch (IOException ex) {
				return Response.status(Status.INTERNAL_SERVER_ERROR).build();
			}
		}

		private Object convertIfNecessary(Object body) throws IOException {
			for (Function<Object, Object> converter : BODY_CONVERTERS) {
				body = converter.apply(body);
			}
			return body;
		}

	}

	/**
	 * Body converter from {@link org.springframework.core.io.Resource} to
	 * {@link InputStream}.
	 */
	private static final class ResourceBodyConverter implements Function<Object, Object> {

		@Override
		public Object apply(Object body) {
			if (body instanceof org.springframework.core.io.Resource) {
				try {
					return ((org.springframework.core.io.Resource) body).getInputStream();
				}
				catch (IOException ex) {
					throw new IllegalStateException();
				}
			}
			return body;
		}

	}

	/**
	 * Body converter from {@link Mono} to {@link Mono#block()}.
	 */
	private static final class MonoBodyConverter implements Function<Object, Object> {

		@Override
		public Object apply(Object body) {
			if (body instanceof Mono) {
				return ((Mono<?>) body).block();
			}
			return body;
		}

	}

	/**
	 * {@link Inflector} to for endpoint links.
	 */
	private static final class EndpointLinksInflector
			implements Inflector<ContainerRequestContext, Response> {

		private final Collection<EndpointInfo<WebOperation>> endpoints;

		private final EndpointLinksResolver linksResolver;

		private EndpointLinksInflector(Collection<EndpointInfo<WebOperation>> endpoints,
				EndpointLinksResolver linksResolver) {
			this.endpoints = endpoints;
			this.linksResolver = linksResolver;
		}

		@Override
		public Response apply(ContainerRequestContext request) {
			Map<String, Link> links = this.linksResolver.resolveLinks(this.endpoints,
					request.getUriInfo().getAbsolutePath().toString());
			return Response.ok(Collections.singletonMap("_links", links)).build();
		}

	}

}
