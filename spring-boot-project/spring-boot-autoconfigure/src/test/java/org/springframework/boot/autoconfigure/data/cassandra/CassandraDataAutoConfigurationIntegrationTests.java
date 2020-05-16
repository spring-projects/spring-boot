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

package org.springframework.boot.autoconfigure.data.cassandra;

import java.time.Duration;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.CassandraContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.boot.autoconfigure.cassandra.CassandraAutoConfiguration;
import org.springframework.boot.autoconfigure.data.cassandra.city.City;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.data.cassandra.config.CassandraSessionFactoryBean;
import org.springframework.data.cassandra.config.SchemaAction;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link CassandraDataAutoConfiguration} that require a Cassandra instance.
 *
 * @author Mark Paluch
 * @author Stephane Nicoll
 */
@Testcontainers(disabledWithoutDocker = true)
class CassandraDataAutoConfigurationIntegrationTests {

	@Container
	static final CassandraContainer<?> cassandra = new CassandraContainer<>().withStartupAttempts(5)
			.withStartupTimeout(Duration.ofMinutes(10));

	private AnnotationConfigApplicationContext context;

	@BeforeEach
	void setUp() {
		this.context = new AnnotationConfigApplicationContext();
		TestPropertyValues
				.of("spring.data.cassandra.contact-points=" + cassandra.getContainerIpAddress(),
						"spring.data.cassandra.port=" + cassandra.getFirstMappedPort(),
						"spring.data.cassandra.read-timeout=24000", "spring.data.cassandra.connect-timeout=10000")
				.applyTo(this.context.getEnvironment());
	}

	@AfterEach
	void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	void hasDefaultSchemaActionSet() {
		String cityPackage = City.class.getPackage().getName();
		AutoConfigurationPackages.register(this.context, cityPackage);
		this.context.register(CassandraAutoConfiguration.class, CassandraDataAutoConfiguration.class);
		this.context.refresh();
		CassandraSessionFactoryBean bean = this.context.getBean(CassandraSessionFactoryBean.class);
		assertThat(bean.getSchemaAction()).isEqualTo(SchemaAction.NONE);
	}

	@Test
	void hasRecreateSchemaActionSet() {
		createTestKeyspaceIfNotExists();
		String cityPackage = City.class.getPackage().getName();
		AutoConfigurationPackages.register(this.context, cityPackage);
		TestPropertyValues.of("spring.data.cassandra.schemaAction=recreate_drop_unused",
				"spring.data.cassandra.keyspaceName=boot_test").applyTo(this.context);
		this.context.register(CassandraAutoConfiguration.class, CassandraDataAutoConfiguration.class);
		this.context.refresh();
		CassandraSessionFactoryBean bean = this.context.getBean(CassandraSessionFactoryBean.class);
		assertThat(bean.getSchemaAction()).isEqualTo(SchemaAction.RECREATE_DROP_UNUSED);
	}

	private void createTestKeyspaceIfNotExists() {
		Cluster cluster = Cluster.builder().withoutJMXReporting().withPort(cassandra.getFirstMappedPort())
				.addContactPoint(cassandra.getContainerIpAddress()).build();
		try (Session session = cluster.connect()) {
			session.execute("CREATE KEYSPACE IF NOT EXISTS boot_test"
					+ "  WITH REPLICATION = { 'class' : 'SimpleStrategy', 'replication_factor' : 1 };");
		}
	}

}
