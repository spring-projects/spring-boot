/*
 * Copyright 2012-2024 the original author or authors.
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

package smoketest.data.cassandra;

import java.util.UUID;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.CqlSessionBuilder;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.cassandra.DataCassandraTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.testsupport.container.TestImage;
import org.springframework.context.annotation.Bean;
import org.springframework.data.cassandra.core.CassandraTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke tests for Cassandra with SSL.
 *
 * @author Scott Frederick
 * @author Eddú Meléndez
 */
@Testcontainers(disabledWithoutDocker = true)
@DataCassandraTest(properties = { "spring.cassandra.schema-action=create-if-not-exists",
		"spring.cassandra.connection.connect-timeout=60s", "spring.cassandra.connection.init-query-timeout=60s",
		"spring.cassandra.request.timeout=60s", "spring.cassandra.ssl.bundle=client",
		"spring.ssl.bundle.jks.client.keystore.location=classpath:ssl/test-client.p12",
		"spring.ssl.bundle.jks.client.keystore.password=password",
		"spring.ssl.bundle.jks.client.truststore.location=classpath:ssl/test-ca.p12",
		"spring.ssl.bundle.jks.client.truststore.password=password" })
class SampleCassandraApplicationSslTests {

	@Container
	@ServiceConnection
	static final SecureCassandraContainer cassandra = TestImage.container(SecureCassandraContainer.class);

	@Autowired
	private CassandraTemplate cassandraTemplate;

	@Autowired
	private SampleRepository repository;

	@Test
	void testRepository() {
		SampleEntity entity = new SampleEntity();
		entity.setDescription("Look, new @DataCassandraTest!");
		String id = UUID.randomUUID().toString();
		entity.setId(id);
		SampleEntity savedEntity = this.repository.save(entity);
		SampleEntity getEntity = this.cassandraTemplate.selectOneById(id, SampleEntity.class);
		assertThat(getEntity).isNotNull();
		assertThat(getEntity.getId()).isNotNull();
		assertThat(getEntity.getId()).isEqualTo(savedEntity.getId());
		this.repository.deleteAll();
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
