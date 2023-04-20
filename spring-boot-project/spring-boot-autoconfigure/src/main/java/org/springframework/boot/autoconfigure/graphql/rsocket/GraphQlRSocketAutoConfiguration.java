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

	@Bean
	@ConditionalOnMissingBean
	public GraphQlRSocketHandler graphQlRSocketHandler(ExecutionGraphQlService graphQlService,
			ObjectProvider<RSocketGraphQlInterceptor> interceptors, ObjectMapper objectMapper) {
		return new GraphQlRSocketHandler(graphQlService, interceptors.orderedStream().toList(),
				new Jackson2JsonEncoder(objectMapper));
	}

	@Bean
	@ConditionalOnMissingBean
	public GraphQlRSocketController graphQlRSocketController(GraphQlRSocketHandler handler) {
		return new GraphQlRSocketController(handler);
	}

}
