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

package org.springframework.boot.autoconfigure.graphql.data;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.graphql.Book;
import org.springframework.boot.autoconfigure.graphql.GraphQlAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.QueryByExampleExecutor;
import org.springframework.graphql.ExecutionGraphQlService;
import org.springframework.graphql.data.GraphQlRepository;
import org.springframework.graphql.test.tester.ExecutionGraphQlServiceTester;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link GraphQlQueryByExampleAutoConfiguration}
 *
 * @author Brian Clozel
 */
class GraphQlQueryByExampleAutoConfigurationTests {

	private static final Book book = new Book("42", "Test title", 42, "Test Author");

	private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
		.withConfiguration(
				AutoConfigurations.of(GraphQlAutoConfiguration.class, GraphQlQueryByExampleAutoConfiguration.class))
		.withUserConfiguration(MockRepositoryConfig.class)
		.withPropertyValues("spring.main.web-application-type=servlet");

	@Test
	void shouldRegisterDataFetcherForQueryByExampleRepositories() {
		this.contextRunner.run((context) -> {
			ExecutionGraphQlService graphQlService = context.getBean(ExecutionGraphQlService.class);
			ExecutionGraphQlServiceTester graphQlTester = ExecutionGraphQlServiceTester.create(graphQlService);
			graphQlTester.document("{ bookById(id: 1) {name}}")
				.execute()
				.path("bookById.name")
				.entity(String.class)
				.isEqualTo("Test title");
		});
	}

	@Configuration(proxyBeanMethods = false)
	static class MockRepositoryConfig {

		@Bean
		MockRepository mockRepository() {
			MockRepository mockRepository = mock(MockRepository.class);
			given(mockRepository.findBy(any(), any())).willReturn(Optional.of(book));
			return mockRepository;
		}

	}

	@GraphQlRepository
	interface MockRepository extends CrudRepository<Book, Long>, QueryByExampleExecutor<Book> {

	}

}
