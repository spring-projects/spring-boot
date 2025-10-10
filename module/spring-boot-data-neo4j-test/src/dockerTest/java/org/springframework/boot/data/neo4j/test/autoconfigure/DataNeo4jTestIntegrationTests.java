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

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.testcontainers.service.connection.ServiceConnectionAutoConfiguration;
import org.springframework.boot.testsupport.container.TestImage;
import org.springframework.context.ApplicationContext;
import org.springframework.data.neo4j.core.Neo4jTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.springframework.boot.autoconfigure.AutoConfigurationImportedCondition.importedAutoConfiguration;

/**
 * Integration test for {@link DataNeo4jTest @DataNeo4jTest}.
 *
 * @author Eddú Meléndez
 * @author Stephane Nicoll
 * @author Michael Simons
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
@DataNeo4jTest
@Testcontainers(disabledWithoutDocker = true)
class DataNeo4jTestIntegrationTests {

	@Container
	@ServiceConnection
	static final Neo4jContainer<?> neo4j = TestImage.container(Neo4jContainer.class);

	@Autowired
	private Neo4jTemplate neo4jTemplate;

	@Autowired
	private ExampleRepository exampleRepository;

	@Autowired
	private ApplicationContext applicationContext;

	@Test
	void testRepository() {
		ExampleGraph exampleGraph = new ExampleGraph("Look, new @DataNeo4jTest!");
		assertThat(exampleGraph.getId()).isNull();
		ExampleGraph savedGraph = this.exampleRepository.save(exampleGraph);
		assertThat(savedGraph.getId()).isNotNull();
		assertThat(this.neo4jTemplate.count(ExampleGraph.class)).isOne();
	}

	@Test
	void didNotInjectExampleService() {
		assertThatExceptionOfType(NoSuchBeanDefinitionException.class)
			.isThrownBy(() -> this.applicationContext.getBean(ExampleService.class));
	}

	@Test
	void serviceConnectionAutoConfigurationWasImported() {
		assertThat(this.applicationContext).has(importedAutoConfiguration(ServiceConnectionAutoConfiguration.class));
	}

}
