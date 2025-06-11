/*
 * Copyright 2012-2025 the original author or authors.
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
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
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
import org.springframework.graphql.server.webflux.GraphQlRequestPredicates;
import org.springframework.graphql.server.webflux.GraphQlSseHandler;
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
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.server.support.WebSocketUpgradeHandlerPredicate;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for enabling Spring GraphQL over
 * WebFlux.
 *
 * @author Brian Clozel
 * @since 2.7.0
 */
@AutoConfiguration(after = GraphQlAutoConfiguration.class)
@ConditionalOnWebApplication(type = Type.REACTIVE)
@ConditionalOnClass({ GraphQL.class, GraphQlHttpHandler.class })
@ConditionalOnBean(ExecutionGraphQlService.class)
@EnableConfigurationProperties(GraphQlCorsProperties.class)
@ImportRuntimeHints(GraphQlWebFluxAutoConfiguration.GraphiQlResourceHints.class)
public class GraphQlWebFluxAutoConfiguration {

	private static final Log logger = LogFactory.getLog(GraphQlWebFluxAutoConfiguration.class);

	@Bean
	@ConditionalOnMissingBean
	public GraphQlHttpHandler graphQlHttpHandler(WebGraphQlHandler webGraphQlHandler) {
		return new GraphQlHttpHandler(webGraphQlHandler);
	}

	@Bean
	@ConditionalOnMissingBean
	public GraphQlSseHandler graphQlSseHandler(WebGraphQlHandler webGraphQlHandler, GraphQlProperties properties) {
		return new GraphQlSseHandler(webGraphQlHandler, properties.getHttp().getSse().getTimeout(),
				properties.getHttp().getSse().getKeepAlive());
	}

	@Bean
	@ConditionalOnMissingBean
	public WebGraphQlHandler webGraphQlHandler(ExecutionGraphQlService service,
			ObjectProvider<WebGraphQlInterceptor> interceptors) {
		return WebGraphQlHandler.builder(service).interceptors(interceptors.orderedStream().toList()).build();
	}

	@Bean
	@Order(0)
	public RouterFunction<ServerResponse> graphQlRouterFunction(GraphQlHttpHandler httpHandler,
			GraphQlSseHandler sseHandler, ObjectProvider<GraphQlSource> graphQlSourceProvider,
			GraphQlProperties properties) {
		String path = properties.getHttp().getPath();
		logger.info(LogMessage.format("GraphQL endpoint HTTP POST %s", path));
		RouterFunctions.Builder builder = RouterFunctions.route();
		builder.route(GraphQlRequestPredicates.graphQlHttp(path), httpHandler::handleRequest);
		builder.route(GraphQlRequestPredicates.graphQlSse(path), sseHandler::handleRequest);
		builder.POST(path, this::unsupportedMediaType);
		builder.GET(path, this::onlyAllowPost);
		if (properties.getGraphiql().isEnabled()) {
			GraphiQlHandler graphQlHandler = new GraphiQlHandler(path, properties.getWebsocket().getPath());
			builder.GET(properties.getGraphiql().getPath(), graphQlHandler::handleRequest);
		}
		GraphQlSource graphQlSource = graphQlSourceProvider.getIfAvailable();
		if (properties.getSchema().getPrinter().isEnabled() && graphQlSource != null) {
			SchemaHandler schemaHandler = new SchemaHandler(graphQlSource);
			builder.GET(path + "/schema", schemaHandler::handleRequest);
		}
		return builder.build();
	}

	private Mono<ServerResponse> unsupportedMediaType(ServerRequest request) {
		return ServerResponse.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).headers(this::acceptJson).build();
	}

	private void acceptJson(HttpHeaders headers) {
		headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
	}

	private Mono<ServerResponse> onlyAllowPost(ServerRequest request) {
		return ServerResponse.status(HttpStatus.METHOD_NOT_ALLOWED).headers(this::onlyAllowPost).build();
	}

	private void onlyAllowPost(HttpHeaders headers) {
		headers.setAllow(Collections.singleton(HttpMethod.POST));
	}

	@Configuration(proxyBeanMethods = false)
	public static class GraphQlEndpointCorsConfiguration implements WebFluxConfigurer {

		final GraphQlProperties graphQlProperties;

		final GraphQlCorsProperties corsProperties;

		public GraphQlEndpointCorsConfiguration(GraphQlProperties graphQlProps, GraphQlCorsProperties corsProps) {
			this.graphQlProperties = graphQlProps;
			this.corsProperties = corsProps;
		}

		@Override
		public void addCorsMappings(CorsRegistry registry) {
			CorsConfiguration configuration = this.corsProperties.toCorsConfiguration();
			if (configuration != null) {
				registry.addMapping(this.graphQlProperties.getHttp().getPath()).combine(configuration);
			}
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnProperty("spring.graphql.websocket.path")
	public static class WebSocketConfiguration {

		@Bean
		@ConditionalOnMissingBean
		public GraphQlWebSocketHandler graphQlWebSocketHandler(WebGraphQlHandler webGraphQlHandler,
				GraphQlProperties properties, ServerCodecConfigurer configurer) {
			return new GraphQlWebSocketHandler(webGraphQlHandler, configurer,
					properties.getWebsocket().getConnectionInitTimeout(), properties.getWebsocket().getKeepAlive());
		}

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

	static class GraphiQlResourceHints implements RuntimeHintsRegistrar {

		@Override
		public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
			hints.resources().registerPattern("graphiql/index.html");
		}

	}

}
