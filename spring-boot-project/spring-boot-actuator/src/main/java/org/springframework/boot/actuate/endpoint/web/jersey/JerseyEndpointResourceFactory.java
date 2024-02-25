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

	/**
     * Creates a resource using the provided endpoint mapping and web operation.
     * 
     * @param endpointMapping the endpoint mapping for the resource
     * @param operation the web operation for the resource
     * @return the created resource
     */
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

	/**
     * Returns a resource object based on the provided parameters.
     * 
     * @param endpointMapping The endpoint mapping for the resource.
     * @param operation The web operation for the resource.
     * @param requestPredicate The request predicate for the resource.
     * @param path The path for the resource.
     * @param serverNamespace The server namespace for the resource.
     * @param remainingPathSegmentProvider The remaining path segment provider for the resource.
     * @return The resource object.
     */
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

	/**
     * Creates a resource for endpoint links.
     * 
     * @param endpointPath the path of the endpoint
     * @param endpointMediaTypes the media types supported by the endpoint
     * @param linksResolver the resolver for generating endpoint links
     * @return the created resource
     */
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

		/**
         * Constructs a new OperationInflector with the specified parameters.
         *
         * @param operation the WebOperation to be performed
         * @param readBody a boolean indicating whether the request body should be read
         * @param serverNamespace the WebServerNamespace to be used
         * @param remainingPathSegments the JerseyRemainingPathSegmentProvider to be used
         */
        private OperationInflector(WebOperation operation, boolean readBody, WebServerNamespace serverNamespace,
				JerseyRemainingPathSegmentProvider remainingPathSegments) {
			this.operation = operation;
			this.readBody = readBody;
			this.serverNamespace = serverNamespace;
			this.remainingPathSegmentProvider = remainingPathSegments;
		}

		/**
         * Applies the operation to the given container request context.
         * 
         * @param data the container request context
         * @return the response generated by the operation
         */
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

		/**
         * Extracts the body arguments from the given ContainerRequestContext.
         * 
         * @param data the ContainerRequestContext containing the request data
         * @return a Map containing the extracted body arguments, or an empty Map if no arguments are found
         */
        @SuppressWarnings("unchecked")
		private Map<String, Object> extractBodyArguments(ContainerRequestContext data) {
			Map<String, Object> entity = ((ContainerRequest) data).readEntity(Map.class);
			return (entity != null) ? entity : Collections.emptyMap();
		}

		/**
         * Extracts the path parameters from the given ContainerRequestContext.
         * 
         * @param requestContext The ContainerRequestContext containing the request information.
         * @return A Map containing the extracted path parameters.
         */
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

		/**
         * Retrieves the remaining path segments from the request context or path parameters.
         * 
         * @param requestContext The container request context.
         * @param pathParameters The map of path parameters.
         * @param matchAllRemainingPathSegmentsVariable The variable name for matching all remaining path segments.
         * @return The remaining path segments.
         */
        private String getRemainingPathSegments(ContainerRequestContext requestContext,
				Map<String, Object> pathParameters, String matchAllRemainingPathSegmentsVariable) {
			if (this.remainingPathSegmentProvider != null) {
				return this.remainingPathSegmentProvider.get(requestContext, matchAllRemainingPathSegmentsVariable);
			}
			return (String) pathParameters.get(matchAllRemainingPathSegmentsVariable);
		}

		/**
         * Tokenizes the given path into segments.
         * 
         * @param path the path to be tokenized
         * @return an array of path segments
         */
        private String[] tokenizePathSegments(String path) {
			String[] segments = StringUtils.tokenizeToStringArray(path, PATH_SEPARATOR, false, true);
			for (int i = 0; i < segments.length; i++) {
				if (segments[i].contains("%")) {
					segments[i] = StringUtils.uriDecode(segments[i], StandardCharsets.UTF_8);
				}
			}
			return segments;
		}

		/**
         * Extracts the query parameters from the given ContainerRequestContext.
         * 
         * @param requestContext the ContainerRequestContext containing the query parameters
         * @return a Map containing the extracted query parameters
         */
        private Map<String, Object> extractQueryParameters(ContainerRequestContext requestContext) {
			return extract(requestContext.getUriInfo().getQueryParameters());
		}

		/**
         * Extracts the values from a MultivaluedMap and returns them as a Map.
         * 
         * @param multivaluedMap the MultivaluedMap containing the values to be extracted
         * @return a Map containing the extracted values
         */
        private Map<String, Object> extract(MultivaluedMap<String, String> multivaluedMap) {
			Map<String, Object> result = new HashMap<>();
			multivaluedMap.forEach((name, values) -> {
				if (!CollectionUtils.isEmpty(values)) {
					result.put(name, (values.size() != 1) ? values : values.get(0));
				}
			});
			return result;
		}

		/**
         * Converts the given response object to a JAX-RS Response object based on the HTTP method.
         * 
         * @param response the response object to be converted
         * @param httpMethod the HTTP method used for the request
         * @return the converted JAX-RS Response object
         */
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

		/**
         * Converts the given object if necessary using a list of converters.
         * 
         * @param body the object to be converted
         * @return the converted object
         */
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

		/**
         * Converts the body of a request to an InputStream if it is an instance of org.springframework.core.io.Resource.
         * 
         * @param body the body of the request
         * @return the converted InputStream if the body is an instance of Resource, otherwise returns the original body
         * @throws IllegalStateException if an IOException occurs while getting the InputStream from the Resource
         */
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

		/**
         * Applies the given body object.
         * If the body is an instance of Mono, it blocks and returns the result.
         * Otherwise, it returns the body as is.
         *
         * @param body the body object to apply
         * @return the result of applying the body object
         */
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

		/**
         * Converts the body of a request to a Flux or returns the body as is.
         * 
         * @param body the body of the request
         * @return the converted body as a Flux if it is a Flux, otherwise returns the body as is
         */
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

		/**
         * Constructs a new instance of EndpointLinksInflector with the specified EndpointLinksResolver.
         * 
         * @param linksResolver the EndpointLinksResolver used to resolve endpoint links
         */
        private EndpointLinksInflector(EndpointLinksResolver linksResolver) {
			this.linksResolver = linksResolver;
		}

		/**
         * Applies the links resolver to the given container request context.
         * 
         * @param request the container request context
         * @return the response containing the resolved links
         */
        @Override
		public Response apply(ContainerRequestContext request) {
			Map<String, Link> links = this.linksResolver
				.resolveLinks(request.getUriInfo().getAbsolutePath().toString());
			Map<String, Map<String, Link>> entity = OperationResponseBody.of(Collections.singletonMap("_links", links));
			return Response.ok(entity).build();
		}

	}

	/**
     * JerseySecurityContext class.
     */
    private static final class JerseySecurityContext implements SecurityContext {

		private final jakarta.ws.rs.core.SecurityContext securityContext;

		/**
         * Constructs a new JerseySecurityContext object.
         * 
         * @param securityContext the underlying Jakarta SecurityContext to be wrapped
         */
        private JerseySecurityContext(jakarta.ws.rs.core.SecurityContext securityContext) {
			this.securityContext = securityContext;
		}

		/**
         * Returns the principal associated with this security context.
         * 
         * @return the principal associated with this security context
         */
        @Override
		public Principal getPrincipal() {
			return this.securityContext.getUserPrincipal();
		}

		/**
         * Checks if the current user is in the specified role.
         * 
         * @param role the role to check
         * @return true if the user is in the specified role, false otherwise
         */
        @Override
		public boolean isUserInRole(String role) {
			return this.securityContext.isUserInRole(role);
		}

	}

}
