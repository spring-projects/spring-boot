/*
 * Copyright 2012-2022 the original author or authors.
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

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.CqlSessionBuilder;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.cassandra.CassandraAutoConfiguration;
import org.springframework.boot.autoconfigure.data.cassandra.city.City;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.testsupport.testcontainers.CassandraContainer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.cassandra.config.SchemaAction;
import org.springframework.data.cassandra.config.SessionFactoryFactoryBean;

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
	static final CassandraContainer cassandra = new CassandraContainer();

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(
					AutoConfigurations.of(CassandraAutoConfiguration.class, CassandraDataAutoConfiguration.class))
			.withPropertyValues(
					"spring.data.cassandra.contact-points:" + cassandra.getHost() + ":"
							+ cassandra.getFirstMappedPort(),
					"spring.data.cassandra.local-datacenter=datacenter1",
					"spring.data.cassandra.connection.connect-timeout=60s",
					"spring.data.cassandra.connection.init-query-timeout=60s",
					"spring.data.cassandra.request.timeout=60s")
			.withInitializer((context) -> AutoConfigurationPackages.register((BeanDefinitionRegistry) context,
					City.class.getPackage().getName()));

	@Test
	void hasDefaultSchemaActionSet() {
		this.contextRunner.run((context) -> assertThat(context.getBean(SessionFactoryFactoryBean.class))
				.hasFieldOrPropertyWithValue("schemaAction", SchemaAction.NONE));
	}

	@Test
	void hasRecreateSchemaActionSet() {
		this.contextRunner.withUserConfiguration(KeyspaceTestConfiguration.class)
				.withPropertyValues("spring.data.cassandra.schemaAction=recreate_drop_unused")
				.run((context) -> assertThat(context.getBean(SessionFactoryFactoryBean.class))
						.hasFieldOrPropertyWithValue("schemaAction", SchemaAction.RECREATE_DROP_UNUSED));
	}

	@Configuration(proxyBeanMethods = false)
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
