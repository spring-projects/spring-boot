/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.autoconfigure.data.neo4j;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.neo4j.Neo4jAutoConfiguration;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.core.DatabaseSelection;
import org.springframework.data.neo4j.core.ReactiveDatabaseSelectionProvider;
import org.springframework.data.neo4j.core.ReactiveNeo4jClient;
import org.springframework.data.neo4j.core.ReactiveNeo4jOperations;
import org.springframework.data.neo4j.core.ReactiveNeo4jTemplate;
import org.springframework.data.neo4j.core.convert.Neo4jConversions;
import org.springframework.data.neo4j.core.transaction.ReactiveNeo4jTransactionManager;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.ReactiveTransactionManager;

/**
 * @author Michael J. Simons
 */
class ReactiveNeo4jDataAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withPropertyValues("spring.data.neo4j.repositories.type=reactive")
			.withUserConfiguration(MockedDriverConfiguration.class)
			.withConfiguration(AutoConfigurations.of(Neo4jAutoConfiguration.class, Neo4jDataAutoConfiguration.class));

	@Test
	void shouldProvideConversions() {
		contextRunner.run(ctx -> assertThat(ctx).hasSingleBean(Neo4jConversions.class));
	}

	@Test
	void shouldProvideDefaultDatabaseNameProvider() {
		contextRunner.run(ctx -> {
			assertThat(ctx).hasSingleBean(ReactiveDatabaseSelectionProvider.class);
			ReactiveDatabaseSelectionProvider databaseNameProvider = ctx
					.getBean(ReactiveDatabaseSelectionProvider.class);
			assertThat(databaseNameProvider).isSameAs(ReactiveDatabaseSelectionProvider.getDefaultSelectionProvider());
		});
	}

	@Test
	void shouldProvideStaticDatabaseNameProviderIfConfigured() {
		contextRunner.withPropertyValues("spring.data.neo4j.database=foobar").run(ctx -> {
			assertThat(ctx).hasSingleBean(ReactiveDatabaseSelectionProvider.class);
			ReactiveDatabaseSelectionProvider databaseNameProvider = ctx
					.getBean(ReactiveDatabaseSelectionProvider.class);
			StepVerifier.create(databaseNameProvider.getDatabaseSelection().map(DatabaseSelection::getValue))
					.expectNext("foobar").expectComplete();
		});
	}

	@Test
	void shouldRespectExistingDatabaseNameProvider() {
		contextRunner.withPropertyValues("spring.data.neo4j.database=foobar")
				.withUserConfiguration(ConfigurationWithExistingReactiveDatabaseSelectionProvider.class).run(ctx -> {
					assertThat(ctx).hasSingleBean(ReactiveDatabaseSelectionProvider.class);
					ReactiveDatabaseSelectionProvider databaseNameProvider = ctx
							.getBean(ReactiveDatabaseSelectionProvider.class);
					StepVerifier.create(databaseNameProvider.getDatabaseSelection().map(DatabaseSelection::getValue))
							.expectNext("whatever").expectComplete();
				});
	}

	@Test
	void shouldRequireAllNeededClasses() {
		contextRunner
				.withClassLoader(new FilteredClassLoader(ReactiveNeo4jTransactionManager.class,
						ReactiveTransactionManager.class, Flux.class))
				.run(ctx -> assertThat(ctx).doesNotHaveBean(ReactiveNeo4jClient.class)
						.doesNotHaveBean(ReactiveNeo4jTemplate.class)
						.doesNotHaveBean(ReactiveNeo4jTransactionManager.class));
	}

	@Test
	void shouldCreateNewReactiveNeo4jClient() {
		contextRunner.run(ctx -> assertThat(ctx).hasSingleBean(ReactiveNeo4jClient.class));
	}

	@Test
	void shouldNotReplaceExistingReactiveNeo4jClient() {
		contextRunner.withUserConfiguration(ConfigurationWithExistingReactiveClient.class)
				.run(ctx -> assertThat(ctx).hasSingleBean(ReactiveNeo4jClient.class).hasBean("myCustomReactiveClient"));
	}

	@Test
	void shouldCreateNewNeo4jTemplate() {
		contextRunner.withUserConfiguration(ConfigurationWithExistingReactiveDatabaseSelectionProvider.class)
				.run(ctx -> {
					assertThat(ctx).hasSingleBean(ReactiveNeo4jTemplate.class);

					// Verify that the template uses the provided database name
					// provider
					ReactiveNeo4jTemplate template = ctx.getBean(ReactiveNeo4jTemplate.class);
					ReactiveDatabaseSelectionProvider provider = (ReactiveDatabaseSelectionProvider) ReflectionTestUtils
							.getField(template, "databaseSelectionProvider");
					assertThat(provider).isSameAs(ctx.getBean(ReactiveDatabaseSelectionProvider.class));
				});
	}

	@Test
	void shouldNotReplaceExistingNeo4jTemplate() {
		contextRunner.withUserConfiguration(ConfigurationWithExistingReactiveTemplate.class).run(ctx -> assertThat(ctx)
				.hasSingleBean(ReactiveNeo4jOperations.class).hasBean("myCustomReactiveOperations"));
	}

	@Test
	void shouldCreateNewTransactionManager() {
		contextRunner.withUserConfiguration(ConfigurationWithExistingReactiveDatabaseSelectionProvider.class)
				.run(ctx -> {
					assertThat(ctx).hasSingleBean(ReactiveNeo4jTransactionManager.class);

					// Verify that the transaction manager uses the provided
					// database name provider
					ReactiveNeo4jTransactionManager transactionManager = ctx
							.getBean(ReactiveNeo4jTransactionManager.class);
					ReactiveDatabaseSelectionProvider provider = (ReactiveDatabaseSelectionProvider) ReflectionTestUtils
							.getField(transactionManager, "databaseSelectionProvider");
					assertThat(provider).isSameAs(ctx.getBean(ReactiveDatabaseSelectionProvider.class));
				});
	}

	@Test
	void shouldHonourExistingTransactionManager() {
		contextRunner.withUserConfiguration(ConfigurationWithExistingReactiveTransactionManager.class)
				.run(ctx -> assertThat(ctx).hasSingleBean(ReactiveTransactionManager.class)
						.hasBean("myCustomReactiveTransactionManager"));
	}

	@Configuration
	static class ConfigurationWithExistingReactiveClient {

		@Bean("myCustomReactiveClient")
		ReactiveNeo4jClient neo4jClient(Driver driver) {
			return ReactiveNeo4jClient.create(driver);
		}

	}

	@Configuration
	static class ConfigurationWithExistingReactiveTemplate {

		@Bean("myCustomReactiveOperations")
		ReactiveNeo4jOperations neo4jOperations() {
			return mock(ReactiveNeo4jOperations.class);
		}

	}

	@Configuration
	static class ConfigurationWithExistingReactiveTransactionManager {

		@Bean("myCustomReactiveTransactionManager")
		ReactiveTransactionManager transactionManager() {
			return mock(ReactiveTransactionManager.class);
		}

	}

	@Configuration
	static class ConfigurationWithExistingReactiveDatabaseSelectionProvider {

		@Bean
		ReactiveDatabaseSelectionProvider databaseNameProvider() {
			return () -> Mono.just(DatabaseSelection.byName("whatever"));
		}

	}

}
