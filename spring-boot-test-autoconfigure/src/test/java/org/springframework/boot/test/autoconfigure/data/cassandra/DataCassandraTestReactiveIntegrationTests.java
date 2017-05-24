/*
 * Copyright 2012-2017 the original author or authors.
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

import com.datastax.driver.core.Session;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.cassandra.CassandraTestServer;
import org.springframework.data.cassandra.core.ReactiveCassandraTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sample tests for {@link DataCassandraTest} using reactive repositories.
 *
 * @author Eddú Meléndez
 */
@RunWith(SpringRunner.class)
@DataCassandraTest
@TestPropertySource(properties = {"spring.data.cassandra.schemaAction=recreate_drop_unused",
		"spring.data.cassandra.keyspaceName=boot_test"})
public class DataCassandraTestReactiveIntegrationTests {

	@Rule
	public CassandraTestServer server = new CassandraTestServer();

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Autowired
	private ReactiveCassandraTemplate cassandraTemplate;

	@Autowired
	private ExampleReactiveRepository exampleRepository;

	@Test
	public void testRepository() {
		createTestKeyspaceIfNotExists();
		ExampleTable exampleTable = new ExampleTable();
		exampleTable.setId("1");
		exampleTable.setText("Look, new @DataCassandraTest!");
		exampleTable = this.exampleRepository.save(exampleTable).block();
		assertThat(exampleTable.getId()).isNotNull();
		assertThat(this.cassandraTemplate.count(ExampleTable.class).block())
				.isEqualTo(1);
	}

	private void createTestKeyspaceIfNotExists() {
		Session session = this.server.getCluster().connect();
		try {
			session.execute("CREATE KEYSPACE IF NOT EXISTS boot_test"
					+ "  WITH REPLICATION = { 'class' : 'SimpleStrategy', 'replication_factor' : 1 };");
		}
		finally {
			session.close();
		}
	}

}
