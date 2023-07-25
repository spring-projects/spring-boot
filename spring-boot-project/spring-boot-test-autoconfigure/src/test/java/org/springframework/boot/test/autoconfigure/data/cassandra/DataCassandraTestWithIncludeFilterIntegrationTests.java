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

package org.springframework.boot.test.autoconfigure.data.cassandra;

import java.util.UUID;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.CqlSessionBuilder;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.testsupport.testcontainers.CassandraContainer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.stereotype.Service;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test with custom include filter for
 * {@link DataCassandraTest @DataCassandraTest}.
 *
 * @author Artsiom Yudovin
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
@DataCassandraTest(includeFilters = @Filter(Service.class),
		properties = { "spring.cassandra.schema-action=create-if-not-exists",
				"spring.cassandra.connection.connect-timeout=60s", "spring.cassandra.connection.init-query-timeout=60s",
				"spring.cassandra.request.timeout=60s" })
@Testcontainers(disabledWithoutDocker = true)
class DataCassandraTestWithIncludeFilterIntegrationTests {

	@Container
	@ServiceConnection
	static final CassandraContainer cassandra = new CassandraContainer();

	@Autowired
	private ExampleRepository exampleRepository;

	@Autowired
	private ExampleService service;

	@Test
	void testService() {
		ExampleEntity exampleEntity = new ExampleEntity();
		exampleEntity.setDescription("Look, new @DataCassandraTest!");
		String id = UUID.randomUUID().toString();
		exampleEntity.setId(id);
		this.exampleRepository.save(exampleEntity);
		assertThat(this.service.hasRecord(exampleEntity)).isTrue();
	}

	@TestConfiguration(proxyBeanMethods = false)
	static class KeyspaceTestConfiguration {

		@Bean
		CqlSession cqlSession(CqlSessionBuilder cqlSessionBuilder) {
			try (CqlSession session = cqlSessionBuilder.build()) {
				session.execute("CREATE KEYSPACE IF NOT EXISTS boot_test"
						+ " WITH REPLICATION = { 'class' : 'SimpleStrategy', 'replication_factor' : 1 };");
			}
			return cqlSessionBuilder.withKeyspace("boot_test").build();
		}

	}

}
