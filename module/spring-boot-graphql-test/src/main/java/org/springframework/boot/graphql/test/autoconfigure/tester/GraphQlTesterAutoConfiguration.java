/*
 * Copyright 2012-present the original author or authors.
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
