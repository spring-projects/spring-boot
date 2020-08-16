/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.test.autoconfigure.data.neo4j;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.data.neo4j.core.DatabaseSelectionProvider;
import org.springframework.data.neo4j.core.ReactiveNeo4jTemplate;
import org.springframework.data.neo4j.core.transaction.Neo4jTransactionManager;
import org.springframework.data.neo4j.repository.config.ReactiveNeo4jRepositoryConfigurationExtension;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Integration tests for the reactive SDN/RX Neo4j test slice.
 *
 * @author Michael J. Simons
 * @author Scott Frederick
 * @since 2.4.0
 */
@DataNeo4jTest
@Testcontainers(disabledWithoutDocker = true)
class DataNeo4jTestReactiveIntegrationTests {

	@Container
	static final Neo4jContainer<?> neo4j = new Neo4jContainer<>("neo4j:4.0").withoutAuthentication()
			.withStartupTimeout(Duration.ofMinutes(10));

	@DynamicPropertySource
	static void neo4jProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.neo4j.uri", neo4j::getBoltUrl);
	}

	@Autowired
	private ReactiveNeo4jTemplate neo4jTemplate;

	@Autowired
	private ExampleReactiveRepository exampleRepository;

	@Autowired
	private ApplicationContext applicationContext;

	@Test
	void testRepository() {
		Mono.just(new ExampleGraph("Look, new @DataNeo4jTest with reactive!")).flatMap(this.exampleRepository::save)
				.as(StepVerifier::create).expectNextCount(1).verifyComplete();
		StepVerifier.create(this.neo4jTemplate.count(ExampleGraph.class)).expectNext(1L).verifyComplete();
	}

	@Test
	void didNotInjectExampleService() {
		assertThatExceptionOfType(NoSuchBeanDefinitionException.class)
				.isThrownBy(() -> this.applicationContext.getBean(ExampleService.class));
	}

	// Providing this bean fulfills a requirement that a @Transactional test has a
	// PlatformTransactionManager in the app context (enforced by
	// org.springframework.test.context.transaction.TransactionalTestExecutionListener).
	// Providing a ReactiveNeo4jTransactionManager would be more appropriate, but won't
	// allow the test to succeed.
	@TestConfiguration(proxyBeanMethods = false)
	static class ReactiveTransactionManagerConfiguration {

		@Bean(ReactiveNeo4jRepositoryConfigurationExtension.DEFAULT_TRANSACTION_MANAGER_BEAN_NAME)
		Neo4jTransactionManager reactiveTransactionManager(Driver driver,
				DatabaseSelectionProvider databaseNameProvider) {
			return new Neo4jTransactionManager(driver, databaseNameProvider);
		}

	}

}
