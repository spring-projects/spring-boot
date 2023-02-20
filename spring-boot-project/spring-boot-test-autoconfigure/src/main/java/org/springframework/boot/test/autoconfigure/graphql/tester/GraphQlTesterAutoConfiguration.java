/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.test.autoconfigure.graphql.tester;

import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.GraphQL;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.graphql.GraphQlAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.graphql.ExecutionGraphQlService;
import org.springframework.graphql.test.tester.ExecutionGraphQlServiceTester;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.http.MediaType;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;

/**
 * Auto-configuration for {@link GraphQlTester}.
 *
 * @author Brian Clozel
 * @since 2.7.0
 */
@AutoConfiguration(after = { JacksonAutoConfiguration.class, GraphQlAutoConfiguration.class })
@ConditionalOnClass({ GraphQL.class, GraphQlTester.class })
public class GraphQlTesterAutoConfiguration {

	@Bean
	@ConditionalOnBean(ExecutionGraphQlService.class)
	@ConditionalOnMissingBean
	@SuppressWarnings("removal")
	public ExecutionGraphQlServiceTester graphQlTester(ExecutionGraphQlService graphQlService,
			ObjectProvider<ObjectMapper> objectMapperProvider) {
		ExecutionGraphQlServiceTester.Builder<?> builder = ExecutionGraphQlServiceTester.builder(graphQlService);
		objectMapperProvider.ifAvailable((objectMapper) -> {
			builder.encoder(new Jackson2JsonEncoder(objectMapper, MediaType.APPLICATION_GRAPHQL_RESPONSE,
					MediaType.APPLICATION_JSON, MediaType.APPLICATION_GRAPHQL));
			builder.decoder(new Jackson2JsonDecoder(objectMapper, MediaType.APPLICATION_JSON));
		});
		return builder.build();
	}

}
