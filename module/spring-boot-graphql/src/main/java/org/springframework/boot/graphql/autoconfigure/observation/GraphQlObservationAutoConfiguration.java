/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.graphql.autoconfigure.observation;

import graphql.GraphQL;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.graphql.execution.GraphQlSource;
import org.springframework.graphql.observation.DataFetcherObservationConvention;
import org.springframework.graphql.observation.DataLoaderObservationConvention;
import org.springframework.graphql.observation.ExecutionRequestObservationConvention;
import org.springframework.graphql.observation.GraphQlObservationInstrumentation;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for instrumentation of Spring
 * GraphQL endpoints.
 *
 * @author Brian Clozel
 * @since 4.0.0
 */
@AutoConfiguration(
		afterName = "org.springframework.boot.micrometer.observation.autoconfigure.ObservationAutoConfiguration")
@ConditionalOnBean(ObservationRegistry.class)
@ConditionalOnClass({ GraphQL.class, GraphQlSource.class, Observation.class })
public final class GraphQlObservationAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	GraphQlObservationInstrumentation graphQlObservationInstrumentation(ObservationRegistry observationRegistry,
			ObjectProvider<ExecutionRequestObservationConvention> executionConvention,
			ObjectProvider<DataFetcherObservationConvention> dataFetcherConvention,
			ObjectProvider<DataLoaderObservationConvention> dataLoaderObservationConvention) {
		return new GraphQlObservationInstrumentation(observationRegistry, executionConvention.getIfAvailable(),
				dataFetcherConvention.getIfAvailable(), dataLoaderObservationConvention.getIfAvailable());
	}

}
