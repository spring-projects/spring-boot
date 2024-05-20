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

package org.springframework.boot.testcontainers.service.connection.cassandra;

import com.datastax.oss.driver.api.core.CqlSession;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.cassandra.CassandraAutoConfiguration;
import org.springframework.boot.autoconfigure.cassandra.CassandraConnectionDetails;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.testsupport.testcontainers.CassandraContainer;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link CassandraContainerConnectionDetailsFactory}.
 *
 * @author Andy Wilkinson
 */
@SpringJUnitConfig
@Testcontainers(disabledWithoutDocker = true)
class CassandraContainerConnectionDetailsFactoryTests {

	@Container
	@ServiceConnection
	static final CassandraContainer cassandra = new CassandraContainer();

	@Autowired(required = false)
	private CassandraConnectionDetails connectionDetails;

	@Autowired
	private CqlSession cqlSession;

	@Test
	void connectionCanBeMadeToCassandraContainer() {
		assertThat(this.connectionDetails).isNotNull();
		assertThat(this.cqlSession.getMetadata().getNodes()).hasSize(1);
	}

	@Configuration(proxyBeanMethods = false)
	@ImportAutoConfiguration(CassandraAutoConfiguration.class)
	static class TestConfiguration {

	}

}
