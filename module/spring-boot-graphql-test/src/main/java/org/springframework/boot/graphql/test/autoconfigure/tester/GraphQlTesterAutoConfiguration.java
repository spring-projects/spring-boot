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

package org.springframework.boot.graphql.test.autoconfigure.tester;

import graphql.GraphQL;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.graphql.autoconfigure.GraphQlAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.graphql.ExecutionGraphQlService;
import org.springframework.graphql.test.tester.ExecutionGraphQlServiceTester;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.http.MediaType;
import org.springframework.http.codec.json.JacksonJsonDecoder;
import org.springframework.http.codec.json.JacksonJsonEncoder;

/**
 * Auto-configuration for {@link GraphQlTester}.
 *
 * @author Brian Clozel
 * @since 4.0.0
 */
@AutoConfiguration(after = GraphQlAutoConfiguration.class)
@ConditionalOnClass({ GraphQL.class, GraphQlTester.class })
public final class GraphQlTesterAutoConfiguration {

	private static final MediaType APPLICATION_GRAPHQL = new MediaType("application", "graphql+json");

	@Bean
	@ConditionalOnBean(ExecutionGraphQlService.class)
	@ConditionalOnMissingBean
	ExecutionGraphQlServiceTester graphQlTester(ExecutionGraphQlService graphQlService,
			ObjectProvider<JsonMapper> jsonMapperProvider) {
		ExecutionGraphQlServiceTester.Builder<?> builder = ExecutionGraphQlServiceTester.builder(graphQlService);
		jsonMapperProvider.ifAvailable((objectMapper) -> {
			builder.encoder(new JacksonJsonEncoder(objectMapper, MediaType.APPLICATION_GRAPHQL_RESPONSE,
					MediaType.APPLICATION_JSON, APPLICATION_GRAPHQL));
			builder.decoder(new JacksonJsonDecoder(objectMapper, MediaType.APPLICATION_JSON));
		});
		return builder.build();
	}

}
