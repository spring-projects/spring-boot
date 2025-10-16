/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.data.neo4j.test.autoconfigure;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.neo4j.Neo4jContainer;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.testsupport.container.TestImage;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.data.neo4j.core.ReactiveDatabaseSelectionProvider;
import org.springframework.data.neo4j.core.ReactiveNeo4jTemplate;
import org.springframework.data.neo4j.core.transaction.ReactiveNeo4jTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Integration tests for {@link DataNeo4jTest @DataNeo4jTest} with reactive style.
 *
 * @author Michael J. Simons
 * @author Scott Frederick
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
@DataNeo4jTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Testcontainers(disabledWithoutDocker = true)
class DataNeo4jTestReactiveIntegrationTests {

	@Container
	@ServiceConnection
	static final Neo4jContainer neo4j = TestImage.container(Neo4jContainer.class).withoutAuthentication();

	@Autowired
	private ReactiveNeo4jTemplate neo4jTemplate;

	@Autowired
	private ExampleReactiveRepository exampleRepository;

	@Autowired
	private ApplicationContext applicationContext;

	@Test
	void testRepository() {
		Mono.just(new ExampleGraph("Look, new @DataNeo4jTest with reactive!"))
			.flatMap(this.exampleRepository::save)
			.as(StepVerifier::create)
			.expectNextCount(1)
			.expectComplete()
			.verify(Duration.ofSeconds(30));
		StepVerifier.create(this.neo4jTemplate.count(ExampleGraph.class))
			.expectNext(1L)
			.expectComplete()
			.verify(Duration.ofSeconds(30));
	}

	@Test
	void didNotInjectExampleService() {
		assertThatExceptionOfType(NoSuchBeanDefinitionException.class)
			.isThrownBy(() -> this.applicationContext.getBean(ExampleService.class));
	}

	@TestConfiguration(proxyBeanMethods = false)
	static class ReactiveTransactionManagerConfiguration {

		@Bean
		ReactiveNeo4jTransactionManager reactiveTransactionManager(Driver driver,
				ReactiveDatabaseSelectionProvider databaseNameProvider) {
			return new ReactiveNeo4jTransactionManager(driver, databaseNameProvider);
		}

	}

}
