/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.graphql.autoconfigure.data;

import java.util.Collections;
import java.util.List;

import com.querydsl.core.Query;
import graphql.GraphQL;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.graphql.autoconfigure.GraphQlAutoConfiguration;
import org.springframework.boot.graphql.autoconfigure.GraphQlSourceBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.data.querydsl.ReactiveQuerydslPredicateExecutor;
import org.springframework.graphql.data.query.QuerydslDataFetcher;
import org.springframework.graphql.execution.GraphQlSource;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;

/**
 * {@link EnableAutoConfiguration Auto-configuration} that creates a
 * {@link GraphQlSourceBuilderCustomizer}s to detect Spring Data repositories with
 * Querydsl support and register them as {@code DataFetcher}s for any queries with a
 * matching return type.
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @since 4.0.0
 * @see QuerydslDataFetcher#autoRegistrationConfigurer(List, List)
 */
@AutoConfiguration(after = GraphQlAutoConfiguration.class)
@ConditionalOnClass({ GraphQL.class, Query.class, QuerydslDataFetcher.class, ReactiveQuerydslPredicateExecutor.class })
@ConditionalOnBean(GraphQlSource.class)
public final class GraphQlReactiveQuerydslAutoConfiguration {

	@Bean
	GraphQlSourceBuilderCustomizer reactiveQuerydslRegistrar(
			ObjectProvider<ReactiveQuerydslPredicateExecutor<?>> reactiveExecutors) {
		RuntimeWiringConfigurer configurer = QuerydslDataFetcher.autoRegistrationConfigurer(Collections.emptyList(),
				reactiveExecutors.orderedStream().toList());
		return (builder) -> builder.configureRuntimeWiring(configurer);
	}

}
