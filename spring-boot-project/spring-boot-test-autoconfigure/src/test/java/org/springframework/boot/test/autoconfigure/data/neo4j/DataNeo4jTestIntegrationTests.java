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
import org.neo4j.ogm.session.Session;
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Integration test for {@link DataNeo4jTest @DataNeo4jTest}.
 *
 * @author Eddú Meléndez
 * @author Stephane Nicoll
 * @author Michael Simons
 */
@DataNeo4jTest
@Testcontainers(disabledWithoutDocker = true)
class DataNeo4jTestIntegrationTests {

	@Container
	static final Neo4jContainer<?> neo4j = new Neo4jContainer<>().withoutAuthentication()
			.withStartupTimeout(Duration.ofMinutes(10));

	@Autowired
	private Session session;

	@Autowired
	private ExampleRepository exampleRepository;

	@Autowired
	private ApplicationContext applicationContext;

	@DynamicPropertySource
	static void neo4jProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.data.neo4j.uri", neo4j::getBoltUrl);
	}

	@Test
	void testRepository() {
		ExampleGraph exampleGraph = new ExampleGraph();
		exampleGraph.setDescription("Look, new @DataNeo4jTest!");
		assertThat(exampleGraph.getId()).isNull();
		ExampleGraph savedGraph = this.exampleRepository.save(exampleGraph);
		assertThat(savedGraph.getId()).isNotNull();
		assertThat(this.session.countEntitiesOfType(ExampleGraph.class)).isEqualTo(1);
	}

	@Test
	void didNotInjectExampleService() {
		assertThatExceptionOfType(NoSuchBeanDefinitionException.class)
				.isThrownBy(() -> this.applicationContext.getBean(ExampleService.class));
	}

}
