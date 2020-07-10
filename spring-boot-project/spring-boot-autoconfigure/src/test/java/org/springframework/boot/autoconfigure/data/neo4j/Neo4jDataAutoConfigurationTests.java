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

import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.neo4j.Neo4jAutoConfiguration;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.core.DatabaseSelection;
import org.springframework.data.neo4j.core.DatabaseSelectionProvider;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.data.neo4j.core.Neo4jOperations;
import org.springframework.data.neo4j.core.Neo4jTemplate;
import org.springframework.data.neo4j.core.convert.Neo4jConversions;
import org.springframework.data.neo4j.core.transaction.Neo4jTransactionManager;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * @author Michael J. Simons
 */
class Neo4jDataAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withPropertyValues("spring.data.neo4j.repositories.type=imperative")
			.withUserConfiguration(MockedDriverConfiguration.class)
			.withConfiguration(AutoConfigurations.of(Neo4jAutoConfiguration.class, Neo4jDataAutoConfiguration.class));

	@Test
	void shouldProvideConversions() {
		contextRunner.run(ctx -> assertThat(ctx).hasSingleBean(Neo4jConversions.class));
	}

	@Test
	void shouldProvideDefaultDatabaseNameProvider() {
		contextRunner.run(ctx -> {
			assertThat(ctx).hasSingleBean(DatabaseSelectionProvider.class);
			DatabaseSelectionProvider databaseNameProvider = ctx.getBean(DatabaseSelectionProvider.class);
			assertThat(databaseNameProvider).isSameAs(DatabaseSelectionProvider.getDefaultSelectionProvider());
		});
	}

	@Test
	void shouldProvideStaticDatabaseNameProviderIfConfigured() {
		contextRunner.withPropertyValues("spring.data.neo4j.database=foobar").run(ctx -> {
			assertThat(ctx).hasSingleBean(DatabaseSelectionProvider.class);
			DatabaseSelectionProvider databaseNameProvider = ctx.getBean(DatabaseSelectionProvider.class);
			assertThat(databaseNameProvider.getDatabaseSelection()).isEqualTo(DatabaseSelection.byName("foobar"));
		});
	}

	@Test
	void shouldRespectExistingDatabaseNameProvider() {
		contextRunner.withPropertyValues("spring.data.neo4j.database=foobar")
				.withUserConfiguration(ConfigurationWithExistingDatabaseSelectionProvider.class).run(ctx -> {
					assertThat(ctx).hasSingleBean(DatabaseSelectionProvider.class);
					DatabaseSelectionProvider databaseNameProvider = ctx.getBean(DatabaseSelectionProvider.class);
					assertThat(databaseNameProvider.getDatabaseSelection())
							.isEqualTo(DatabaseSelection.byName("whatever"));
				});
	}

	@Test
	void shouldRequireAllNeededClasses() {
		contextRunner
				.withClassLoader(
						new FilteredClassLoader(Neo4jTransactionManager.class, PlatformTransactionManager.class))
				.run(ctx -> assertThat(ctx).doesNotHaveBean(Neo4jClient.class).doesNotHaveBean(Neo4jTemplate.class)
						.doesNotHaveBean(Neo4jTransactionManager.class));
	}

	@Test
	void shouldCreateNewNeo4jClient() {
		contextRunner.run(ctx -> assertThat(ctx).hasSingleBean(Neo4jClient.class));
	}

	@Test
	void shouldNotReplaceExistingNeo4jClient() {
		contextRunner.withUserConfiguration(ConfigurationWithExistingClient.class)
				.run(ctx -> assertThat(ctx).hasSingleBean(Neo4jClient.class).hasBean("myCustomClient"));
	}

	@Test
	void shouldCreateNewNeo4jTemplate() {
		contextRunner.withUserConfiguration(ConfigurationWithExistingDatabaseSelectionProvider.class).run(ctx -> {
			assertThat(ctx).hasSingleBean(Neo4jTemplate.class);

			// Verify that the template uses the provided database name
			// provider
			Neo4jTemplate template = ctx.getBean(Neo4jTemplate.class);
			DatabaseSelectionProvider provider = (DatabaseSelectionProvider) ReflectionTestUtils.getField(template,
					"databaseSelectionProvider");
			assertThat(provider).isSameAs(ctx.getBean(DatabaseSelectionProvider.class));
		});
	}

	@Test
	void shouldNotReplaceExistingNeo4jTemplate() {
		contextRunner.withUserConfiguration(ConfigurationWithExistingTemplate.class)
				.run(ctx -> assertThat(ctx).hasSingleBean(Neo4jOperations.class).hasBean("myCustomOperations"));
	}

	@Test
	void shouldCreateNewTransactionManager() {
		contextRunner.withUserConfiguration(ConfigurationWithExistingDatabaseSelectionProvider.class).run(ctx -> {
			assertThat(ctx).hasSingleBean(Neo4jTransactionManager.class);

			// Verify that the transaction manager uses the provided
			// database name provider
			Neo4jTransactionManager transactionManager = ctx.getBean(Neo4jTransactionManager.class);
			DatabaseSelectionProvider provider = (DatabaseSelectionProvider) ReflectionTestUtils
					.getField(transactionManager, "databaseSelectionProvider");
			assertThat(provider).isSameAs(ctx.getBean(DatabaseSelectionProvider.class));
		});
	}

	@Test
	void shouldHonourExistingTransactionManager() {
		contextRunner.withUserConfiguration(ConfigurationWithExistingTransactionManager.class)
				.run(ctx -> assertThat(ctx).hasSingleBean(PlatformTransactionManager.class)
						.hasBean("myCustomTransactionManager"));
	}

	@Configuration
	static class ConfigurationWithExistingClient {

		@Bean("myCustomClient")
		Neo4jClient neo4jClient(Driver driver) {
			return Neo4jClient.create(driver);
		}

	}

	@Configuration
	static class ConfigurationWithExistingTemplate {

		@Bean("myCustomOperations")
		Neo4jOperations neo4jOperations() {
			return mock(Neo4jOperations.class);
		}

	}

	@Configuration
	static class ConfigurationWithExistingTransactionManager {

		@Bean("myCustomTransactionManager")
		PlatformTransactionManager transactionManager() {
			return mock(PlatformTransactionManager.class);
		}

	}

	@Configuration
	static class ConfigurationWithExistingDatabaseSelectionProvider {

		@Bean
		DatabaseSelectionProvider databaseSelectionProvider() {
			return () -> DatabaseSelection.byName("whatever");
		}

	}

}
