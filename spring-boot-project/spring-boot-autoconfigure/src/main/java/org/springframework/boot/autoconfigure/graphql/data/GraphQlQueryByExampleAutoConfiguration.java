/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.boot.autoconfigure.graphql.data;

import java.util.List;
import java.util.stream.Collectors;

import graphql.GraphQL;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.graphql.GraphQlAutoConfiguration;
import org.springframework.boot.autoconfigure.graphql.GraphQlSourceBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.repository.query.QueryByExampleExecutor;
import org.springframework.data.repository.query.ReactiveQueryByExampleExecutor;
import org.springframework.graphql.data.query.QueryByExampleDataFetcher;
import org.springframework.graphql.execution.GraphQlSource;

/**
 * {@link EnableAutoConfiguration Auto-configuration} that creates a
 * {@link GraphQlSourceBuilderCustomizer}s to detect Spring Data repositories with Query
 * By Example support and register them as {@code DataFetcher}s for any queries with a
 * matching return type.
 *
 * @author Rossen Stoyanchev
 * @since 2.7.0
 * @see QueryByExampleDataFetcher#autoRegistrationConfigurer(List, List)
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({ GraphQL.class, QueryByExampleDataFetcher.class, QueryByExampleExecutor.class })
@ConditionalOnBean(GraphQlSource.class)
@AutoConfigureAfter(GraphQlAutoConfiguration.class)
public class GraphQlQueryByExampleAutoConfiguration {

	@Bean
	public GraphQlSourceBuilderCustomizer queryByExampleRegistrar(
			ObjectProvider<QueryByExampleExecutor<?>> executorsProvider,
			ObjectProvider<ReactiveQueryByExampleExecutor<?>> reactiveExecutorsProvider) {

		return (builder) -> {
			List<QueryByExampleExecutor<?>> executors = executorsProvider.stream().collect(Collectors.toList());
			List<ReactiveQueryByExampleExecutor<?>> reactiveExecutors = reactiveExecutorsProvider.stream()
					.collect(Collectors.toList());
			if (!executors.isEmpty()) {
				builder.configureRuntimeWiring(
						QueryByExampleDataFetcher.autoRegistrationConfigurer(executors, reactiveExecutors));
			}
		};
	}

}
