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

package org.springframework.boot.test.autoconfigure.data.cassandra;

import java.time.Duration;
import java.util.UUID;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.CqlSessionBuilder;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.CassandraContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.redis.ExampleService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testsupport.testcontainers.DockerImageNames;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.data.cassandra.core.CassandraTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Integration test for {@link DataCassandraTest @DataCassandraTest}.
 *
 * @author Artsiom Yudovin
 */
@DataCassandraTest(properties = { "spring.data.cassandra.local-datacenter=datacenter1",
		"spring.data.cassandra.schema-action=create-if-not-exists",
		"spring.data.cassandra.connection.connect-timeout=20s",
		"spring.data.cassandra.connection.init-query-timeout=2s", "spring.data.cassandra.request.timeout=10s" })
@Testcontainers(disabledWithoutDocker = true)
class DataCassandraTestIntegrationTests {

	@Container
	static final CassandraContainer<?> cassandra = new CassandraContainer<>(DockerImageNames.cassandra())
			.withStartupAttempts(5).withStartupTimeout(Duration.ofMinutes(10));

	@DynamicPropertySource
	static void cassandraProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.data.cassandra.contact-points",
				() -> cassandra.getHost() + ":" + cassandra.getFirstMappedPort());
	}

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
		ExampleEntity entity = new ExampleEntity();
		entity.setDescription("Look, new @DataCassandraTest!");
		String id = UUID.randomUUID().toString();
		entity.setId(id);
		ExampleEntity savedEntity = this.exampleRepository.save(entity);
		ExampleEntity getEntity = this.cassandraTemplate.selectOneById(id, ExampleEntity.class);
		assertThat(getEntity).isNotNull();
		assertThat(getEntity.getId()).isNotNull();
		assertThat(getEntity.getId()).isEqualTo(savedEntity.getId());
		this.exampleRepository.deleteAll();
	}

	@TestConfiguration(proxyBeanMethods = false)
	static class KeyspaceTestConfiguration {

		@Bean
		CqlSession cqlSession(CqlSessionBuilder cqlSessionBuilder) {
			try (CqlSession session = cqlSessionBuilder.build()) {
				session.execute("CREATE KEYSPACE IF NOT EXISTS boot_test"
						+ "  WITH REPLICATION = { 'class' : 'SimpleStrategy', 'replication_factor' : 1 };");
			}
			return cqlSessionBuilder.withKeyspace("boot_test").build();
		}

	}

}
