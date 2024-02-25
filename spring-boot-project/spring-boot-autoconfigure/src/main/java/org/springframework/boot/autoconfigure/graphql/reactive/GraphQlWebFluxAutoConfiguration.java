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

package org.springframework.boot.autoconfigure.graphql.reactive;

import java.util.Collections;

import graphql.GraphQL;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.core.publisher.Mono;

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
import org.springframework.graphql.server.webflux.GraphQlHttpHandler;
import org.springframework.graphql.server.webflux.GraphQlWebSocketHandler;
import org.springframework.graphql.server.webflux.GraphiQlHandler;
import org.springframework.graphql.server.webflux.SchemaHandler;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.config.CorsRegistry;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.reactive.function.server.RequestPredicate;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.server.support.WebSocketUpgradeHandlerPredicate;

import static org.springframework.web.reactive.function.server.RequestPredicates.accept;
import static org.springframework.web.reactive.function.server.RequestPredicates.contentType;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for enabling Spring GraphQL over
 * WebFlux.
 *
 * @author Brian Clozel
 * @since 2.7.0
 */
@AutoConfiguration(after = GraphQlAutoConfiguration.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
@ConditionalOnClass({ GraphQL.class, GraphQlHttpHandler.class })
@ConditionalOnBean(ExecutionGraphQlService.class)
@EnableConfigurationProperties(GraphQlCorsProperties.class)
@ImportRuntimeHints(GraphQlWebFluxAutoConfiguration.GraphiQlResourceHints.class)
public class GraphQlWebFluxAutoConfiguration {

	@SuppressWarnings("removal")
	private static final RequestPredicate SUPPORTS_MEDIATYPES = accept(MediaType.APPLICATION_GRAPHQL_RESPONSE,
			MediaType.APPLICATION_JSON, MediaType.APPLICATION_GRAPHQL)
		.and(contentType(MediaType.APPLICATION_JSON));

	private static final Log logger = LogFactory.getLog(GraphQlWebFluxAutoConfiguration.class);

	/**
     * Creates a new instance of {@link GraphQlHttpHandler} if no other bean of the same type is present.
     * 
     * @param webGraphQlHandler the {@link WebGraphQlHandler} to be used by the {@link GraphQlHttpHandler}
     * @return the created {@link GraphQlHttpHandler}
     */
    @Bean
	@ConditionalOnMissingBean
	public GraphQlHttpHandler graphQlHttpHandler(WebGraphQlHandler webGraphQlHandler) {
		return new GraphQlHttpHandler(webGraphQlHandler);
	}

	/**
     * Creates a new instance of {@link WebGraphQlHandler} if there is no existing bean of the same type.
     * 
     * @param service the {@link ExecutionGraphQlService} to be used by the handler
     * @param interceptors the {@link WebGraphQlInterceptor} objects to be used by the handler
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
     *
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
		builder = builder.POST(path, SUPPORTS_MEDIATYPES, httpHandler::handleRequest);
		if (properties.getGraphiql().isEnabled()) {
			GraphiQlHandler graphQlHandler = new GraphiQlHandler(path, properties.getWebsocket().getPath());
			builder = builder.GET(properties.getGraphiql().getPath(), graphQlHandler::handleRequest);
		}
		if (properties.getSchema().getPrinter().isEnabled()) {
			SchemaHandler schemaHandler = new SchemaHandler(graphQlSource);
			builder = builder.GET(path + "/schema", schemaHandler::handleRequest);
		}
		return builder.build();
	}

	/**
     * Returns a Mono of ServerResponse with a status of METHOD_NOT_ALLOWED and headers that only allow POST requests.
     * 
     * @param request the ServerRequest object representing the incoming request
     * @return a Mono of ServerResponse with the appropriate status and headers
     */
    private Mono<ServerResponse> onlyAllowPost(ServerRequest request) {
		return ServerResponse.status(HttpStatus.METHOD_NOT_ALLOWED).headers(this::onlyAllowPost).build();
	}

	/**
     * Sets the allowed HTTP methods to only include POST.
     *
     * @param headers the HttpHeaders object to modify
     */
    private void onlyAllowPost(HttpHeaders headers) {
		headers.setAllow(Collections.singleton(HttpMethod.POST));
	}

	/**
     * GraphQlEndpointCorsConfiguration class.
     */
    @Configuration(proxyBeanMethods = false)
	public static class GraphQlEndpointCorsConfiguration implements WebFluxConfigurer {

		final GraphQlProperties graphQlProperties;

		final GraphQlCorsProperties corsProperties;

		/**
         * Constructs a new GraphQlEndpointCorsConfiguration with the specified GraphQlProperties and GraphQlCorsProperties.
         * 
         * @param graphQlProps the GraphQlProperties to be used for configuring the GraphQL endpoint
         * @param corsProps the GraphQlCorsProperties to be used for configuring the CORS settings of the GraphQL endpoint
         */
        public GraphQlEndpointCorsConfiguration(GraphQlProperties graphQlProps, GraphQlCorsProperties corsProps) {
			this.graphQlProperties = graphQlProps;
			this.corsProperties = corsProps;
		}

		/**
         * Adds CORS mappings to the provided CorsRegistry.
         * 
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
	@ConditionalOnProperty(prefix = "spring.graphql.websocket", name = "path")
	public static class WebSocketConfiguration {

		/**
         * Creates a new instance of the GraphQlWebSocketHandler bean if no other bean of the same type exists.
         * 
         * @param webGraphQlHandler The WebGraphQlHandler bean to be used by the GraphQlWebSocketHandler.
         * @param properties The GraphQlProperties bean containing the configuration properties.
         * @param configurer The ServerCodecConfigurer bean used for configuring the server codec.
         * @return The newly created GraphQlWebSocketHandler bean.
         */
        @Bean
		@ConditionalOnMissingBean
		public GraphQlWebSocketHandler graphQlWebSocketHandler(WebGraphQlHandler webGraphQlHandler,
				GraphQlProperties properties, ServerCodecConfigurer configurer) {
			return new GraphQlWebSocketHandler(webGraphQlHandler, configurer,
					properties.getWebsocket().getConnectionInitTimeout());
		}

		/**
         * Creates a WebSocket endpoint for the GraphQL API.
         * 
         * @param graphQlWebSocketHandler The handler for the GraphQL WebSocket endpoint.
         * @param properties              The properties for the GraphQL configuration.
         * @return                        The handler mapping for the GraphQL WebSocket endpoint.
         */
        @Bean
		public HandlerMapping graphQlWebSocketEndpoint(GraphQlWebSocketHandler graphQlWebSocketHandler,
				GraphQlProperties properties) {
			String path = properties.getWebsocket().getPath();
			logger.info(LogMessage.format("GraphQL endpoint WebSocket %s", path));
			SimpleUrlHandlerMapping mapping = new SimpleUrlHandlerMapping();
			mapping.setHandlerPredicate(new WebSocketUpgradeHandlerPredicate());
			mapping.setUrlMap(Collections.singletonMap(path, graphQlWebSocketHandler));
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
         * 
         * @param hints the runtime hints for GraphiQL resources
         * @param classLoader the class loader to use for loading resources
         */
        @Override
		public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
			hints.resources().registerPattern("graphiql/index.html");
		}

	}

}
