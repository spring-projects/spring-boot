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

package org.springframework.boot.autoconfigure.graphql.rsocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.GraphQL;
import io.rsocket.core.RSocketServer;
import reactor.netty.http.server.HttpServer;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.graphql.GraphQlAutoConfiguration;
import org.springframework.boot.autoconfigure.rsocket.RSocketMessagingAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.graphql.ExecutionGraphQlService;
import org.springframework.graphql.data.method.annotation.support.AnnotatedControllerConfigurer;
import org.springframework.graphql.execution.GraphQlSource;
import org.springframework.graphql.server.GraphQlRSocketHandler;
import org.springframework.graphql.server.RSocketGraphQlInterceptor;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.messaging.rsocket.annotation.support.RSocketMessageHandler;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for enabling Spring GraphQL over
 * RSocket.
 *
 * @author Brian Clozel
 * @since 2.7.0
 */
@AutoConfiguration(after = { GraphQlAutoConfiguration.class, RSocketMessagingAutoConfiguration.class })
@ConditionalOnClass({ GraphQL.class, GraphQlSource.class, RSocketServer.class, HttpServer.class })
@ConditionalOnBean({ RSocketMessageHandler.class, AnnotatedControllerConfigurer.class })
@ConditionalOnProperty(prefix = "spring.graphql.rsocket", name = "mapping")
public class GraphQlRSocketAutoConfiguration {

	/**
     * Creates a new instance of {@link GraphQlRSocketHandler} if no other bean of the same type is present.
     * 
     * @param graphQlService the {@link ExecutionGraphQlService} to be used by the handler
     * @param interceptors an {@link ObjectProvider} of {@link RSocketGraphQlInterceptor} to be applied to the handler
     * @param objectMapper the {@link ObjectMapper} to be used for JSON encoding
     * @return a new instance of {@link GraphQlRSocketHandler}
     */
    @Bean
	@ConditionalOnMissingBean
	public GraphQlRSocketHandler graphQlRSocketHandler(ExecutionGraphQlService graphQlService,
			ObjectProvider<RSocketGraphQlInterceptor> interceptors, ObjectMapper objectMapper) {
		return new GraphQlRSocketHandler(graphQlService, interceptors.orderedStream().toList(),
				new Jackson2JsonEncoder(objectMapper));
	}

	/**
     * Creates a new instance of {@link GraphQlRSocketController} if no other bean of the same type is present.
     * 
     * @param handler the {@link GraphQlRSocketHandler} to be used by the controller
     * @return a new instance of {@link GraphQlRSocketController}
     */
    @Bean
	@ConditionalOnMissingBean
	public GraphQlRSocketController graphQlRSocketController(GraphQlRSocketHandler handler) {
		return new GraphQlRSocketController(handler);
	}

}
