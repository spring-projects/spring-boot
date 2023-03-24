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

import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import org.glassfish.jersey.process.Inflector;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.model.Resource;
import org.glassfish.jersey.server.model.Resource.Builder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.boot.actuate.endpoint.InvalidEndpointRequestException;
import org.springframework.boot.actuate.endpoint.InvocationContext;
import org.springframework.boot.actuate.endpoint.OperationArgumentResolver;
import org.springframework.boot.actuate.endpoint.OperationResponseBody;
import org.springframework.boot.actuate.endpoint.ProducibleOperationArgumentResolver;
import org.springframework.boot.actuate.endpoint.SecurityContext;
import org.springframework.boot.actuate.endpoint.web.EndpointLinksResolver;
import org.springframework.boot.actuate.endpoint.web.EndpointMapping;
import org.springframework.boot.actuate.endpoint.web.EndpointMediaTypes;
import org.springframework.boot.actuate.endpoint.web.ExposableWebEndpoint;
import org.springframework.boot.actuate.endpoint.web.Link;
import org.springframework.boot.actuate.endpoint.web.WebEndpointResponse;
import org.springframework.boot.actuate.endpoint.web.WebOperation;
import org.springframework.boot.actuate.endpoint.web.WebOperationRequestPredicate;
import org.springframework.boot.actuate.endpoint.web.WebServerNamespace;
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
		endpoints.stream()
			.flatMap((endpoint) -> endpoint.getOperations().stream())
			.map((operation) -> createResource(endpointMapping, operation))
			.forEach(resources::add);
		if (shouldRegisterLinks) {
			Resource resource = createEndpointLinksResource(endpointMapping.getPath(), endpointMediaTypes,
					linksResolver);
			resources.add(resource);
		}
		return resources;
	}

	protected Resource createResource(EndpointMapping endpointMapping, WebOperation operation) {
		WebOperationRequestPredicate requestPredicate = operation.getRequestPredicate();
		String path = requestPredicate.getPath();
		String matchAllRemainingPathSegmentsVariable = requestPredicate.getMatchAllRemainingPathSegmentsVariable();
		if (matchAllRemainingPathSegmentsVariable != null) {
			path = path.replace("{*" + matchAllRemainingPathSegmentsVariable + "}",
					"{" + matchAllRemainingPathSegmentsVariable + ": .*}");
		}
		return getResource(endpointMapping, operation, requestPredicate, path, null, null);
	}

	protected Resource getResource(EndpointMapping endpointMapping, WebOperation operation,
			WebOperationRequestPredicate requestPredicate, String path, WebServerNamespace serverNamespace,
			JerseyRemainingPathSegmentProvider remainingPathSegmentProvider) {
		Builder resourceBuilder = Resource.builder()
			.path(endpointMapping.getPath())
			.path(endpointMapping.createSubPath(path));
		resourceBuilder.addMethod(requestPredicate.getHttpMethod().name())
			.consumes(StringUtils.toStringArray(requestPredicate.getConsumes()))
			.produces(StringUtils.toStringArray(requestPredicate.getProduces()))
			.handledBy(new OperationInflector(operation, !requestPredicate.getConsumes().isEmpty(), serverNamespace,
					remainingPathSegmentProvider));
		return resourceBuilder.build();
	}

	private Resource createEndpointLinksResource(String endpointPath, EndpointMediaTypes endpointMediaTypes,
			EndpointLinksResolver linksResolver) {
		Builder resourceBuilder = Resource.builder().path(endpointPath);
		resourceBuilder.addMethod("GET")
			.produces(StringUtils.toStringArray(endpointMediaTypes.getProduced()))
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
				converters.add(new FluxBodyConverter());
				converters.add(new MonoBodyConverter());
			}
			BODY_CONVERTERS = Collections.unmodifiableList(converters);
		}

		private final WebOperation operation;

		private final boolean readBody;

		private final WebServerNamespace serverNamespace;

		private final JerseyRemainingPathSegmentProvider remainingPathSegmentProvider;

		private OperationInflector(WebOperation operation, boolean readBody, WebServerNamespace serverNamespace,
				JerseyRemainingPathSegmentProvider remainingPathSegments) {
			this.operation = operation;
			this.readBody = readBody;
			this.serverNamespace = serverNamespace;
			this.remainingPathSegmentProvider = remainingPathSegments;
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
				JerseySecurityContext securityContext = new JerseySecurityContext(data.getSecurityContext());
				OperationArgumentResolver serverNamespaceArgumentResolver = OperationArgumentResolver
					.of(WebServerNamespace.class, () -> this.serverNamespace);
				InvocationContext invocationContext = new InvocationContext(securityContext, arguments,
						serverNamespaceArgumentResolver,
						new ProducibleOperationArgumentResolver(() -> data.getHeaders().get("Accept")));
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
				String remainingPathSegments = getRemainingPathSegments(requestContext, pathParameters,
						matchAllRemainingPathSegmentsVariable);
				pathParameters.put(matchAllRemainingPathSegmentsVariable, tokenizePathSegments(remainingPathSegments));
			}
			return pathParameters;
		}

		private String getRemainingPathSegments(ContainerRequestContext requestContext,
				Map<String, Object> pathParameters, String matchAllRemainingPathSegmentsVariable) {
			if (this.remainingPathSegmentProvider != null) {
				return this.remainingPathSegmentProvider.get(requestContext, matchAllRemainingPathSegmentsVariable);
			}
			return (String) pathParameters.get(matchAllRemainingPathSegmentsVariable);
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
			if (!(response instanceof WebEndpointResponse<?> webEndpointResponse)) {
				return Response.status(Status.OK).entity(convertIfNecessary(response)).build();
			}
			return Response.status(webEndpointResponse.getStatus())
				.header("Content-Type", webEndpointResponse.getContentType())
				.entity(convertIfNecessary(webEndpointResponse.getBody()))
				.build();
		}

		private Object convertIfNecessary(Object body) {
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
	 * Body converter from {@link Flux} to {@link Flux#collectList Mono&lt;List&gt;}.
	 */
	private static final class FluxBodyConverter implements Function<Object, Object> {

		@Override
		public Object apply(Object body) {
			if (body instanceof Flux) {
				return ((Flux<?>) body).collectList();
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
			Map<String, Map<String, Link>> entity = OperationResponseBody.of(Collections.singletonMap("_links", links));
			return Response.ok(entity).build();
		}

	}

	private static final class JerseySecurityContext implements SecurityContext {

		private final jakarta.ws.rs.core.SecurityContext securityContext;

		private JerseySecurityContext(jakarta.ws.rs.core.SecurityContext securityContext) {
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
