/*
 * Copyright 2012-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.test.autoconfigure.data.cassandra;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.data.cassandra.config.SchemaAction;

abstract class AbstractCassandraInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {


	@Override
	public final void initialize(ConfigurableApplicationContext configurableApplicationContext) {
		TestPropertyValues.of("spring.data.cassandra.port=" + getMappedPort(),
				"spring.data.cassandra.schemaAction=" + SchemaAction.RECREATE_DROP_UNUSED,
				"spring.data.cassandra.keyspaceName=boot_test")
				.applyTo(configurableApplicationContext.getEnvironment());

		createTestKeyspaceIfNotExists();
	}

	abstract int getMappedPort();


	private void createTestKeyspaceIfNotExists() {
		Cluster cluster = Cluster.builder().withPort(getMappedPort())
				.addContactPoint("localhost").build();
		try (Session session = cluster.connect()) {
			session.execute("CREATE KEYSPACE IF NOT EXISTS boot_test"
					+ "  WITH REPLICATION = { 'class' : 'SimpleStrategy', 'replication_factor' : 1 };");
		}
	}

}
