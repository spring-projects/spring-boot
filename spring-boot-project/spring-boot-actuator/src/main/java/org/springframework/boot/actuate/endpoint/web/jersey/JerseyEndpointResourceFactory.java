/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.actuate.endpoint.web.jersey;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
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

import org.springframework.boot.actuate.endpoint.InvalidEndpointRequestException;
import org.springframework.boot.actuate.endpoint.InvocationContext;
import org.springframework.boot.actuate.endpoint.SecurityContext;
import org.springframework.boot.actuate.endpoint.http.ApiVersion;
import org.springframework.boot.actuate.endpoint.web.EndpointLinksResolver;
import org.springframework.boot.actuate.endpoint.web.EndpointMapping;
import org.springframework.boot.actuate.endpoint.web.EndpointMediaTypes;
import org.springframework.boot.actuate.endpoint.web.ExposableWebEndpoint;
import org.springframework.boot.actuate.endpoint.web.Link;
import org.springframework.boot.actuate.endpoint.web.WebEndpointResponse;
import org.springframework.boot.actuate.endpoint.web.WebOperation;
import org.springframework.boot.actuate.endpoint.web.WebOperationRequestPredicate;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * A factory for creating Jersey {@link Resource Resources} for {@link WebOperation web
 * endpoint operations}.
 *
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @since 2.0.0
 */
public class JerseyEndpointResourceFactory {

	/**
	 * Creates {@link Resource Resources} for the operations of the given
	 * {@code webEndpoints}.
	 * @param endpointMapping the base mapping for all endpoints
	 * @param endpoints the web endpoints
	 * @param endpointMediaTypes media types consumed and produced by the endpoints
	 * @param linksResolver resolver for determining links to available endpoints
	 * @param shouldRegisterLinks should register links
	 * @return the resources for the operations
	 */
	public Collection<Resource> createEndpointResources(EndpointMapping endpointMapping,
			Collection<ExposableWebEndpoint> endpoints, EndpointMediaTypes endpointMediaTypes,
			EndpointLinksResolver linksResolver, boolean shouldRegisterLinks) {
		List<Resource> resources = new ArrayList<>();
		endpoints.stream().flatMap((endpoint) -> endpoint.getOperations().stream())
				.map((operation) -> createResource(endpointMapping, operation)).forEach(resources::add);
		if (shouldRegisterLinks) {
			Resource resource = createEndpointLinksResource(endpointMapping.getPath(), endpointMediaTypes,
					linksResolver);
			resources.add(resource);
		}
		return resources;
	}

	private Resource createResource(EndpointMapping endpointMapping, WebOperation operation) {
		WebOperationRequestPredicate requestPredicate = operation.getRequestPredicate();
		String path = requestPredicate.getPath();
		String matchAllRemainingPathSegmentsVariable = requestPredicate.getMatchAllRemainingPathSegmentsVariable();
		if (matchAllRemainingPathSegmentsVariable != null) {
			path = path.replace("{*" + matchAllRemainingPathSegmentsVariable + "}",
					"{" + matchAllRemainingPathSegmentsVariable + ": .*}");
		}
		Builder resourceBuilder = Resource.builder().path(endpointMapping.createSubPath(path));
		resourceBuilder.addMethod(requestPredicate.getHttpMethod().name())
				.consumes(StringUtils.toStringArray(requestPredicate.getConsumes()))
				.produces(StringUtils.toStringArray(requestPredicate.getProduces()))
				.handledBy(new OperationInflector(operation, !requestPredicate.getConsumes().isEmpty()));
		return resourceBuilder.build();
	}

	private Resource createEndpointLinksResource(String endpointPath, EndpointMediaTypes endpointMediaTypes,
			EndpointLinksResolver linksResolver) {
		Builder resourceBuilder = Resource.builder().path(endpointPath);
		resourceBuilder.addMethod("GET").produces(StringUtils.toStringArray(endpointMediaTypes.getProduced()))
				.handledBy(new EndpointLinksInflector(linksResolver));
		return resourceBuilder.build();
	}

	/**
	 * {@link Inflector} to invoke the {@link WebOperation}.
	 */
	private static final class OperationInflector implements Inflector<ContainerRequestContext, Object> {

		private static final String PATH_SEPARATOR = AntPathMatcher.DEFAULT_PATH_SEPARATOR;

		private static final List<Function<Object, Object>> BODY_CONVERTERS;

		static {
			List<Function<Object, Object>> converters = new ArrayList<>();
			converters.add(new ResourceBodyConverter());
			if (ClassUtils.isPresent("reactor.core.publisher.Mono", OperationInflector.class.getClassLoader())) {
				converters.add(new MonoBodyConverter());
			}
			BODY_CONVERTERS = Collections.unmodifiableList(converters);
		}

		private final WebOperation operation;

		private final boolean readBody;

		private OperationInflector(WebOperation operation, boolean readBody) {
			this.operation = operation;
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
				ApiVersion apiVersion = ApiVersion.fromHttpHeaders(data.getHeaders());
				JerseySecurityContext securityContext = new JerseySecurityContext(data.getSecurityContext());
				InvocationContext invocationContext = new InvocationContext(apiVersion, securityContext, arguments);
				Object response = this.operation.invoke(invocationContext);
				return convertToJaxRsResponse(response, data.getRequest().getMethod());
			}
			catch (InvalidEndpointRequestException ex) {
				return Response.status(Status.BAD_REQUEST).build();
			}
		}

		@SuppressWarnings("unchecked")
		private Map<String, Object> extractBodyArguments(ContainerRequestContext data) {
			Map<String, Object> entity = ((ContainerRequest) data).readEntity(Map.class);
			return (entity != null) ? entity : Collections.emptyMap();
		}

		private Map<String, Object> extractPathParameters(ContainerRequestContext requestContext) {
			Map<String, Object> pathParameters = extract(requestContext.getUriInfo().getPathParameters());
			String matchAllRemainingPathSegmentsVariable = this.operation.getRequestPredicate()
					.getMatchAllRemainingPathSegmentsVariable();
			if (matchAllRemainingPathSegmentsVariable != null) {
				String remainingPathSegments = (String) pathParameters.get(matchAllRemainingPathSegmentsVariable);
				pathParameters.put(matchAllRemainingPathSegmentsVariable, tokenizePathSegments(remainingPathSegments));
			}
			return pathParameters;
		}

		private String[] tokenizePathSegments(String path) {
			String[] segments = StringUtils.tokenizeToStringArray(path, PATH_SEPARATOR, false, true);
			for (int i = 0; i < segments.length; i++) {
				if (segments[i].contains("%")) {
					segments[i] = StringUtils.uriDecode(segments[i], StandardCharsets.UTF_8);
				}
			}
			return segments;
		}

		private Map<String, Object> extractQueryParameters(ContainerRequestContext requestContext) {
			return extract(requestContext.getUriInfo().getQueryParameters());
		}

		private Map<String, Object> extract(MultivaluedMap<String, String> multivaluedMap) {
			Map<String, Object> result = new HashMap<>();
			multivaluedMap.forEach((name, values) -> {
				if (!CollectionUtils.isEmpty(values)) {
					result.put(name, (values.size() != 1) ? values : values.get(0));
				}
			});
			return result;
		}

		private Response convertToJaxRsResponse(Object response, String httpMethod) {
			if (response == null) {
				boolean isGet = HttpMethod.GET.equals(httpMethod);
				Status status = isGet ? Status.NOT_FOUND : Status.NO_CONTENT;
				return Response.status(status).build();
			}
			try {
				if (!(response instanceof WebEndpointResponse)) {
					return Response.status(Status.OK).entity(convertIfNecessary(response)).build();
				}
				WebEndpointResponse<?> webEndpointResponse = (WebEndpointResponse<?>) response;
				return Response.status(webEndpointResponse.getStatus())
						.entity(convertIfNecessary(webEndpointResponse.getBody())).build();
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
	private static final class EndpointLinksInflector implements Inflector<ContainerRequestContext, Response> {

		private final EndpointLinksResolver linksResolver;

		private EndpointLinksInflector(EndpointLinksResolver linksResolver) {
			this.linksResolver = linksResolver;
		}

		@Override
		public Response apply(ContainerRequestContext request) {
			Map<String, Link> links = this.linksResolver
					.resolveLinks(request.getUriInfo().getAbsolutePath().toString());
			return Response.ok(Collections.singletonMap("_links", links)).build();
		}

	}

	private static final class JerseySecurityContext implements SecurityContext {

		private final javax.ws.rs.core.SecurityContext securityContext;

		private JerseySecurityContext(javax.ws.rs.core.SecurityContext securityContext) {
			this.securityContext = securityContext;
		}

		@Override
		public Principal getPrincipal() {
			return this.securityContext.getUserPrincipal();
		}

		@Override
		public boolean isUserInRole(String role) {
			return this.securityContext.isUserInRole(role);
		}

	}

}
