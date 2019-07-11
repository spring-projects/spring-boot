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

package org.springframework.boot.test.autoconfigure.data.cassandra;

import java.util.UUID;

import com.datastax.driver.core.Cluster;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.redis.ExampleService;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.boot.testsupport.testcontainers.CassandraContainer;
import org.springframework.boot.testsupport.testcontainers.DisabledWithoutDockerTestcontainers;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.data.cassandra.core.CassandraTemplate;
import org.springframework.test.context.ContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Integration test for {@link DataCassandraTest @DataCassandraTest}.
 *
 * @author Artsiom Yudovin
 */
@DisabledWithoutDockerTestcontainers
@ContextConfiguration(initializers = DataCassandraTestIntegrationTests.Initializer.class)
@DataCassandraTest
class DataCassandraTestIntegrationTests {

	@Container
	public static CassandraContainer cassandra = new CassandraContainer();

	@Autowired
	private CassandraTemplate cassandraTemplate;

	@Autowired
	private ExampleRepository exampleRepository;

	@Autowired
	private ApplicationContext applicationContext;

	@Test
	void didNotInjectExampleService() {
		assertThatExceptionOfType(NoSuchBeanDefinitionException.class)
				.isThrownBy(() -> this.applicationContext.getBean(ExampleService.class));
	}

	@Test
	void testRepository() {

		Person person = new Person();
		person.setDescription("Look, new @DataCassandraTest!");
		String id = UUID.randomUUID().toString();
		person.setId(id);

		Person savedEntity = this.exampleRepository.save(person);
		Person getEntity = this.cassandraTemplate.selectOneById(id, Person.class);

		assertThat(getEntity).isNotNull();
		assertThat(getEntity.getId()).isNotNull();
		assertThat(getEntity.getId()).isEqualTo(savedEntity.getId());

		this.exampleRepository.deleteAll();
	}

	static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

		@Override
		public void initialize(ConfigurableApplicationContext configurableApplicationContext) {
			TestPropertyValues.of("spring.data.cassandra.port=" + cassandra.getFirstMappedPort())
					.and("spring.data.cassandra.keyspaceName=test")
					.and("spring.data.cassandra.schema-action=CREATE_IF_NOT_EXISTS")
					.applyTo(configurableApplicationContext.getEnvironment());

			Cluster cluster = Cluster.builder().withoutJMXReporting()
					.addContactPoints(cassandra.getContainerIpAddress()).withPort(cassandra.getFirstMappedPort())
					.build();
			cluster.connect().execute("CREATE KEYSPACE test "
					+ "WITH replication = {'class':'SimpleStrategy', 'replication_factor' : 1};");
		}

	}

}
