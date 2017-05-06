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

package org.springframework.boot.test.autoconfigure.data.neo4j;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.neo4j.ogm.session.Session;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for {@link DataNeo4jTest}.
 *
 * @author Eddú Meléndez
 * @author Stephane Nicoll
 */
@RunWith(SpringRunner.class)
@DataNeo4jTest
public class DataNeo4jTestIntegrationTests {

	@Rule
	public Neo4jTestServer server = new Neo4jTestServer(
			new String[] { "org.springframework.boot.test.autoconfigure.data.neo4j" });

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Autowired
	private Session session;

	@Autowired
	private ExampleRepository exampleRepository;

	@Autowired
	private ApplicationContext applicationContext;

	@Test
	public void testRepository() {
		ExampleGraph exampleGraph = new ExampleGraph();
		exampleGraph.setDescription("Look, new @DataNeo4jTest!");
		assertThat(exampleGraph.getId()).isNull();
		ExampleGraph savedGraph = this.exampleRepository.save(exampleGraph);
		assertThat(savedGraph.getId()).isNotNull();
		assertThat(this.session.countEntitiesOfType(ExampleGraph.class)).isEqualTo(1);
	}

	@Test
	public void didNotInjectExampleService() {
		this.thrown.expect(NoSuchBeanDefinitionException.class);
		this.applicationContext.getBean(ExampleService.class);
	}

}
