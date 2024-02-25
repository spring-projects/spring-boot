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

package org.springframework.boot.autoconfigure.graphql.servlet;

import java.util.Collections;
import java.util.Map;

import graphql.GraphQL;
import jakarta.websocket.server.ServerContainer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.graphql.GraphQlAutoConfiguration;
import org.springframework.boot.autoconfigure.graphql.GraphQlCorsProperties;
import org.springframework.boot.autoconfigure.graphql.GraphQlProperties;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.core.annotation.Order;
import org.springframework.core.log.LogMessage;
import org.springframework.graphql.ExecutionGraphQlService;
import org.springframework.graphql.execution.GraphQlSource;
import org.springframework.graphql.server.WebGraphQlHandler;
import org.springframework.graphql.server.WebGraphQlInterceptor;
import org.springframework.graphql.server.webmvc.GraphQlHttpHandler;
import org.springframework.graphql.server.webmvc.GraphQlWebSocketHandler;
import org.springframework.graphql.server.webmvc.GraphiQlHandler;
import org.springframework.graphql.server.webmvc.SchemaHandler;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.GenericHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.function.RequestPredicates;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;
import org.springframework.web.socket.server.support.WebSocketHandlerMapping;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for enabling Spring GraphQL over
 * Spring MVC.
 *
 * @author Brian Clozel
 * @since 2.7.0
 */
@AutoConfiguration(after = GraphQlAutoConfiguration.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass({ GraphQL.class, GraphQlHttpHandler.class })
@ConditionalOnBean(ExecutionGraphQlService.class)
@EnableConfigurationProperties(GraphQlCorsProperties.class)
@ImportRuntimeHints(GraphQlWebMvcAutoConfiguration.GraphiQlResourceHints.class)
public class GraphQlWebMvcAutoConfiguration {

	private static final Log logger = LogFactory.getLog(GraphQlWebMvcAutoConfiguration.class);

	@SuppressWarnings("removal")
	private static final MediaType[] SUPPORTED_MEDIA_TYPES = new MediaType[] { MediaType.APPLICATION_GRAPHQL_RESPONSE,
			MediaType.APPLICATION_JSON, MediaType.APPLICATION_GRAPHQL };

	/**
	 * Creates a new instance of {@link GraphQlHttpHandler} if no other bean of the same
	 * type is present.
	 * @param webGraphQlHandler the {@link WebGraphQlHandler} to be used by the
	 * {@link GraphQlHttpHandler}
	 * @return the created {@link GraphQlHttpHandler}
	 */
	@Bean
	@ConditionalOnMissingBean
	public GraphQlHttpHandler graphQlHttpHandler(WebGraphQlHandler webGraphQlHandler) {
		return new GraphQlHttpHandler(webGraphQlHandler);
	}

	/**
	 * Creates a new instance of {@link WebGraphQlHandler} if no other bean of the same
	 * type is present.
	 * @param service the {@link ExecutionGraphQlService} to be used by the handler
	 * @param interceptors the {@link WebGraphQlInterceptor}s to be applied to the handler
	 * @return a new instance of {@link WebGraphQlHandler}
	 */
	@Bean
	@ConditionalOnMissingBean
	public WebGraphQlHandler webGraphQlHandler(ExecutionGraphQlService service,
			ObjectProvider<WebGraphQlInterceptor> interceptors) {
		return WebGraphQlHandler.builder(service).interceptors(interceptors.orderedStream().toList()).build();
	}

	/**
	 * Creates a RouterFunction for handling GraphQL requests.
	 * @param httpHandler the GraphQlHttpHandler to handle the GraphQL requests
	 * @param graphQlSource the GraphQlSource containing the GraphQL schema and resolvers
	 * @param properties the GraphQlProperties containing the configuration properties
	 * @return the RouterFunction for handling GraphQL requests
	 */
	@Bean
	@Order(0)
	public RouterFunction<ServerResponse> graphQlRouterFunction(GraphQlHttpHandler httpHandler,
			GraphQlSource graphQlSource, GraphQlProperties properties) {
		String path = properties.getPath();
		logger.info(LogMessage.format("GraphQL endpoint HTTP POST %s", path));
		RouterFunctions.Builder builder = RouterFunctions.route();
		builder = builder.GET(path, this::onlyAllowPost);
		builder = builder.POST(path, RequestPredicates.contentType(MediaType.APPLICATION_JSON)
			.and(RequestPredicates.accept(SUPPORTED_MEDIA_TYPES)), httpHandler::handleRequest);
		if (properties.getGraphiql().isEnabled()) {
			GraphiQlHandler graphiQLHandler = new GraphiQlHandler(path, properties.getWebsocket().getPath());
			builder = builder.GET(properties.getGraphiql().getPath(), graphiQLHandler::handleRequest);
		}
		if (properties.getSchema().getPrinter().isEnabled()) {
			SchemaHandler schemaHandler = new SchemaHandler(graphQlSource);
			builder = builder.GET(path + "/schema", schemaHandler::handleRequest);
		}
		return builder.build();
	}

	/**
	 * Generates a ServerResponse with status code 405 (METHOD_NOT_ALLOWED) and sets the
	 * headers to only allow POST requests.
	 * @param request the ServerRequest object representing the incoming request
	 * @return a ServerResponse object with status code 405 and headers set to only allow
	 * POST requests
	 */
	private ServerResponse onlyAllowPost(ServerRequest request) {
		return ServerResponse.status(HttpStatus.METHOD_NOT_ALLOWED).headers(this::onlyAllowPost).build();
	}

	/**
	 * Sets the allowed HTTP methods to only include POST.
	 * @param headers the HttpHeaders object to modify
	 */
	private void onlyAllowPost(HttpHeaders headers) {
		headers.setAllow(Collections.singleton(HttpMethod.POST));
	}

	/**
	 * GraphQlEndpointCorsConfiguration class.
	 */
	@Configuration(proxyBeanMethods = false)
	public static class GraphQlEndpointCorsConfiguration implements WebMvcConfigurer {

		final GraphQlProperties graphQlProperties;

		final GraphQlCorsProperties corsProperties;

		/**
		 * Constructs a new GraphQlEndpointCorsConfiguration with the specified
		 * GraphQlProperties and GraphQlCorsProperties.
		 * @param graphQlProps the GraphQlProperties to be used for configuring the
		 * GraphQL endpoint
		 * @param corsProps the GraphQlCorsProperties to be used for configuring the CORS
		 * settings of the GraphQL endpoint
		 */
		public GraphQlEndpointCorsConfiguration(GraphQlProperties graphQlProps, GraphQlCorsProperties corsProps) {
			this.graphQlProperties = graphQlProps;
			this.corsProperties = corsProps;
		}

		/**
		 * Adds CORS mappings to the provided CorsRegistry.
		 * @param registry the CorsRegistry to add the CORS mappings to
		 */
		@Override
		public void addCorsMappings(CorsRegistry registry) {
			CorsConfiguration configuration = this.corsProperties.toCorsConfiguration();
			if (configuration != null) {
				registry.addMapping(this.graphQlProperties.getPath()).combine(configuration);
			}
		}

	}

	/**
	 * WebSocketConfiguration class.
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass({ ServerContainer.class, WebSocketHandler.class })
	@ConditionalOnProperty(prefix = "spring.graphql.websocket", name = "path")
	public static class WebSocketConfiguration {

		/**
		 * Creates a new instance of GraphQlWebSocketHandler if no other bean of the same
		 * type is present.
		 * @param webGraphQlHandler The WebGraphQlHandler instance to be used by the
		 * WebSocketHandler.
		 * @param properties The GraphQlProperties instance containing configuration
		 * properties.
		 * @param converters The HttpMessageConverters instance containing the converters
		 * to be used for message conversion.
		 * @return The newly created GraphQlWebSocketHandler instance.
		 */
		@Bean
		@ConditionalOnMissingBean
		public GraphQlWebSocketHandler graphQlWebSocketHandler(WebGraphQlHandler webGraphQlHandler,
				GraphQlProperties properties, HttpMessageConverters converters) {
			return new GraphQlWebSocketHandler(webGraphQlHandler, getJsonConverter(converters),
					properties.getWebsocket().getConnectionInitTimeout());
		}

		/**
		 * Returns the JSON converter from the given list of HttpMessageConverters.
		 * @param converters the list of HttpMessageConverters
		 * @return the JSON converter
		 * @throws IllegalStateException if no JSON converter is found
		 */
		private GenericHttpMessageConverter<Object> getJsonConverter(HttpMessageConverters converters) {
			return converters.getConverters()
				.stream()
				.filter(this::canReadJsonMap)
				.findFirst()
				.map(this::asGenericHttpMessageConverter)
				.orElseThrow(() -> new IllegalStateException("No JSON converter"));
		}

		/**
		 * Determines if the given HttpMessageConverter can read a JSON map.
		 * @param candidate the HttpMessageConverter to check
		 * @return true if the HttpMessageConverter can read a JSON map, false otherwise
		 */
		private boolean canReadJsonMap(HttpMessageConverter<?> candidate) {
			return candidate.canRead(Map.class, MediaType.APPLICATION_JSON);
		}

		/**
		 * Casts the given HttpMessageConverter to a GenericHttpMessageConverter<Object>.
		 * @param converter the HttpMessageConverter to cast
		 * @return the casted GenericHttpMessageConverter<Object>
		 */
		@SuppressWarnings("unchecked")
		private GenericHttpMessageConverter<Object> asGenericHttpMessageConverter(HttpMessageConverter<?> converter) {
			return (GenericHttpMessageConverter<Object>) converter;
		}

		/**
		 * Creates a WebSocketHandlerMapping bean for handling GraphQL WebSocket requests.
		 * @param handler the GraphQlWebSocketHandler to handle the WebSocket requests
		 * @param properties the GraphQlProperties containing the WebSocket path
		 * configuration
		 * @return the created WebSocketHandlerMapping bean
		 */
		@Bean
		public HandlerMapping graphQlWebSocketMapping(GraphQlWebSocketHandler handler, GraphQlProperties properties) {
			String path = properties.getWebsocket().getPath();
			logger.info(LogMessage.format("GraphQL endpoint WebSocket %s", path));
			WebSocketHandlerMapping mapping = new WebSocketHandlerMapping();
			mapping.setWebSocketUpgradeMatch(true);
			mapping.setUrlMap(Collections.singletonMap(path,
					handler.initWebSocketHttpRequestHandler(new DefaultHandshakeHandler())));
			mapping.setOrder(-2); // Ahead of HTTP endpoint ("routerFunctionMapping" bean)
			return mapping;
		}

	}

	/**
	 * GraphiQlResourceHints class.
	 */
	static class GraphiQlResourceHints implements RuntimeHintsRegistrar {

		/**
		 * Registers the hints for GraphiQL resources.
		 * @param hints the runtime hints for GraphiQL resources
		 * @param classLoader the class loader to use for loading resources
		 */
		@Override
		public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
			hints.resources().registerPattern("graphiql/index.html");
		}

	}

}
