/*
 * Copyright 2012-2021 the original author or authors.
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

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.TestAutoConfigurationPackage;
import org.springframework.boot.autoconfigure.data.neo4j.scan.TestNode;
import org.springframework.boot.autoconfigure.data.neo4j.scan.TestNonAnnotated;
import org.springframework.boot.autoconfigure.data.neo4j.scan.TestPersistent;
import org.springframework.boot.autoconfigure.data.neo4j.scan.TestRelationshipProperties;
import org.springframework.boot.autoconfigure.neo4j.Neo4jAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.core.DatabaseSelection;
import org.springframework.data.neo4j.core.ReactiveDatabaseSelectionProvider;
import org.springframework.data.neo4j.core.ReactiveNeo4jClient;
import org.springframework.data.neo4j.core.ReactiveNeo4jOperations;
import org.springframework.data.neo4j.core.ReactiveNeo4jTemplate;
import org.springframework.data.neo4j.core.mapping.Neo4jMappingContext;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.TransactionManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link Neo4jReactiveDataAutoConfiguration}.
 *
 * @author Michael J. Simons
 * @author Stephane Nicoll
 */
class Neo4jReactiveDataAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withUserConfiguration(MockedDriverConfiguration.class)
			.withConfiguration(AutoConfigurations.of(Neo4jAutoConfiguration.class, Neo4jDataAutoConfiguration.class,
					Neo4jReactiveDataAutoConfiguration.class));

	@Test
	void shouldProvideDefaultDatabaseNameProvider() {
		this.contextRunner.run((context) -> {
			assertThat(context).hasSingleBean(ReactiveDatabaseSelectionProvider.class);
			assertThat(context.getBean(ReactiveDatabaseSelectionProvider.class))
					.isSameAs(ReactiveDatabaseSelectionProvider.getDefaultSelectionProvider());
		});
	}

	@Test
	void shouldUseDatabaseNameIfSet() {
		this.contextRunner.withPropertyValues("spring.data.neo4j.database=test").run((context) -> {
			assertThat(context).hasSingleBean(ReactiveDatabaseSelectionProvider.class);
			StepVerifier.create(context.getBean(ReactiveDatabaseSelectionProvider.class).getDatabaseSelection())
					.consumeNextWith((databaseSelection) -> assertThat(databaseSelection.getValue()).isEqualTo("test"))
					.expectComplete();
		});
	}

	@Test
	void shouldReuseExistingDatabaseNameProvider() {
		this.contextRunner.withPropertyValues("spring.data.neo4j.database=ignored")
				.withUserConfiguration(CustomReactiveDatabaseSelectionProviderConfiguration.class).run((context) -> {
					assertThat(context).hasSingleBean(ReactiveDatabaseSelectionProvider.class);
					StepVerifier.create(context.getBean(ReactiveDatabaseSelectionProvider.class).getDatabaseSelection())
							.consumeNextWith(
									(databaseSelection) -> assertThat(databaseSelection.getValue()).isEqualTo("custom"))
							.expectComplete();
				});
	}

	@Test
	void shouldProvideReactiveNeo4jClient() {
		this.contextRunner.run((context) -> assertThat(context).hasSingleBean(ReactiveNeo4jClient.class));
	}

	@Test
	void shouldProvideReactiveNeo4jClientWithCustomDatabaseSelectionProvider() {
		this.contextRunner.withUserConfiguration(CustomReactiveDatabaseSelectionProviderConfiguration.class)
				.run((context) -> {
					assertThat(context).hasSingleBean(ReactiveNeo4jClient.class);
					assertThat(context.getBean(ReactiveNeo4jClient.class)).extracting("databaseSelectionProvider")
							.isSameAs(context.getBean(ReactiveDatabaseSelectionProvider.class));
				});
	}

	@Test
	void shouldReuseExistingReactiveNeo4jClient() {
		this.contextRunner
				.withBean("myCustomReactiveClient", ReactiveNeo4jClient.class, () -> mock(ReactiveNeo4jClient.class))
				.run((context) -> assertThat(context).hasSingleBean(ReactiveNeo4jClient.class)
						.hasBean("myCustomReactiveClient"));
	}

	@Test
	void shouldProvideReactiveNeo4jTemplate() {
		this.contextRunner.withUserConfiguration(CustomReactiveDatabaseSelectionProviderConfiguration.class)
				.run((context) -> assertThat(context).hasSingleBean(ReactiveNeo4jTemplate.class));
	}

	@Test
	void shouldReuseExistingReactiveNeo4jTemplate() {
		this.contextRunner
				.withBean("myCustomReactiveOperations", ReactiveNeo4jOperations.class,
						() -> mock(ReactiveNeo4jOperations.class))
				.run((context) -> assertThat(context).hasSingleBean(ReactiveNeo4jOperations.class)
						.hasBean("myCustomReactiveOperations"));
	}

	@Test
	void shouldUseExistingReactiveTransactionManager() {
		this.contextRunner
				.withBean("myCustomReactiveTransactionManager", ReactiveTransactionManager.class,
						() -> mock(ReactiveTransactionManager.class))
				.run((context) -> assertThat(context).hasSingleBean(ReactiveTransactionManager.class)
						.hasSingleBean(TransactionManager.class));
	}

	@Test
	void shouldFilterInitialEntityScanWithKnownAnnotations() {
		this.contextRunner.withUserConfiguration(EntityScanConfig.class).run((context) -> {
			Neo4jMappingContext mappingContext = context.getBean(Neo4jMappingContext.class);
			assertThat(mappingContext.hasPersistentEntityFor(TestNode.class)).isTrue();
			assertThat(mappingContext.hasPersistentEntityFor(TestPersistent.class)).isFalse();
			assertThat(mappingContext.hasPersistentEntityFor(TestRelationshipProperties.class)).isTrue();
			assertThat(mappingContext.hasPersistentEntityFor(TestNonAnnotated.class)).isFalse();
		});
	}

	@Configuration(proxyBeanMethods = false)
	static class CustomReactiveDatabaseSelectionProviderConfiguration {

		@Bean
		ReactiveDatabaseSelectionProvider databaseNameProvider() {
			return () -> Mono.just(DatabaseSelection.byName("custom"));
		}

	}

	@Configuration(proxyBeanMethods = false)
	@TestAutoConfigurationPackage(TestPersistent.class)
	static class EntityScanConfig {

	}

}
