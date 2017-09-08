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
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.stereotype.Service;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test with custom include filter for {@link DataNeo4jTest}.
 *
 * @author Eddú Meléndez
 */
@RunWith(SpringRunner.class)
@DataNeo4jTest(includeFilters = @Filter(Service.class))
public class DataNeo4jTestWithIncludeFilterIntegrationTests {

	@Rule
	public Neo4jTestServer server = new Neo4jTestServer(
			new String[] { "org.springframework.boot.test.autoconfigure.data.neo4j" });

	@Autowired
	private ExampleService service;

	@Test
	public void testService() {
		assertThat(this.service.hasNode(ExampleGraph.class)).isFalse();
	}

}
