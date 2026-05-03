/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.graphql.autoconfigure.rsocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.GraphQL;
import io.rsocket.core.RSocketServer;
import reactor.netty.http.server.HttpServer;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.graphql.autoconfigure.GraphQlAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.codec.Encoder;
import org.springframework.graphql.ExecutionGraphQlService;
import org.springframework.graphql.data.method.annotation.support.AnnotatedControllerConfigurer;
import org.springframework.graphql.execution.GraphQlSource;
import org.springframework.graphql.server.GraphQlRSocketHandler;
import org.springframework.graphql.server.RSocketGraphQlInterceptor;
import org.springframework.http.codec.json.JacksonJsonEncoder;
import org.springframework.messaging.rsocket.annotation.support.RSocketMessageHandler;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for enabling Spring GraphQL over
 * RSocket.
 *
 * @author Brian Clozel
 * @since 4.0.0
 */
@AutoConfiguration(after = GraphQlAutoConfiguration.class,
		afterName = { "org.springframework.boot.rsocket.autoconfigure.RSocketMessagingAutoConfiguration",
				"org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration",
				"org.springframework.boot.jackson2.autoconfigure.Jackson2AutoConfiguration" })
@ConditionalOnClass({ GraphQL.class, GraphQlSource.class, RSocketServer.class, HttpServer.class })
@ConditionalOnBean({ RSocketMessageHandler.class, AnnotatedControllerConfigurer.class })
@ConditionalOnProperty("spring.graphql.rsocket.mapping")
public final class GraphQlRSocketAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	GraphQlRSocketHandler graphQlRSocketHandler(ExecutionGraphQlService graphQlService,
			ObjectProvider<RSocketGraphQlInterceptor> interceptors, JsonEncoderSupplier jsonEncoderSupplier) {
		return new GraphQlRSocketHandler(graphQlService, interceptors.orderedStream().toList(),
				jsonEncoderSupplier.jsonEncoder());
	}

	@Bean
	@ConditionalOnMissingBean
	GraphQlRSocketController graphQlRSocketController(GraphQlRSocketHandler handler) {
		return new GraphQlRSocketController(handler);
	}

	interface JsonEncoderSupplier {

		Encoder<?> jsonEncoder();

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnBean(JsonMapper.class)
	@ConditionalOnProperty(name = "spring.graphql.rsocket.preferred-json-mapper", havingValue = "jackson",
			matchIfMissing = true)
	static class JacksonJsonEncoderSupplierConfiguration {

		@Bean
		JsonEncoderSupplier jacksonJsonEncoderSupplier(JsonMapper jsonMapper) {
			return () -> new JacksonJsonEncoder(jsonMapper);
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnBean(ObjectMapper.class)
	@Conditional(NoJacksonOrJackson2Preferred.class)
	@Deprecated(since = "4.0.0", forRemoval = true)
	@SuppressWarnings("removal")
	static class Jackson2JsonEncoderSupplierConfiguration {

		@Bean
		JsonEncoderSupplier jackson2JsonEncoderSupplier(ObjectMapper objectMapper) {
			return () -> new org.springframework.http.codec.json.Jackson2JsonEncoder(objectMapper);
		}

	}

	static class NoJacksonOrJackson2Preferred extends AnyNestedCondition {

		NoJacksonOrJackson2Preferred() {
			super(ConfigurationPhase.PARSE_CONFIGURATION);
		}

		@ConditionalOnMissingClass("tools.jackson.databind.json.JsonMapper")
		static class NoJackson {

		}

		@ConditionalOnProperty(name = "spring.graphql.rsocket.preferred-json-mapper", havingValue = "jackson2")
		static class Jackson2Preferred {

		}

	}

}
