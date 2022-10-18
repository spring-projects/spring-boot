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

package org.springframework.boot.autoconfigure.graphql.data;

import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.graphql.GraphQlSourceBuilderCustomizer;
import org.springframework.graphql.execution.GraphQlSource;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;

/**
 * {@link GraphQlSourceBuilderCustomizer} to apply auto-configured QueryDSL
 * {@link RuntimeWiringConfigurer RuntimeWiringConfigurers}.
 *
 * @param <E> the executor type
 * @param <R> the reactive executor type
 * @author Phillip Webb
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 */
class GraphQlQuerydslSourceBuilderCustomizer<E, R> implements GraphQlSourceBuilderCustomizer {

	private final BiFunction<List<E>, List<R>, RuntimeWiringConfigurer> wiringConfigurerFactory;

	private final List<E> executors;

	private final List<R> reactiveExecutors;

	GraphQlQuerydslSourceBuilderCustomizer(
			BiFunction<List<E>, List<R>, RuntimeWiringConfigurer> wiringConfigurerFactory, ObjectProvider<E> executors,
			ObjectProvider<R> reactiveExecutors) {
		this.wiringConfigurerFactory = wiringConfigurerFactory;
		this.executors = asList(executors);
		this.reactiveExecutors = asList(reactiveExecutors);
	}

	private static <T> List<T> asList(ObjectProvider<T> provider) {
		return (provider != null) ? provider.orderedStream().toList() : Collections.emptyList();
	}

	@Override
	public void customize(GraphQlSource.SchemaResourceBuilder builder) {
		if (!this.executors.isEmpty() || !this.reactiveExecutors.isEmpty()) {
			builder.configureRuntimeWiring(this.wiringConfigurerFactory.apply(this.executors, this.reactiveExecutors));
		}
	}

}
