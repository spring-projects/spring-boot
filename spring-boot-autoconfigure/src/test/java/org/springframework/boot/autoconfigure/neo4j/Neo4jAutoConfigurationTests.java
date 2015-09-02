/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.autoconfigure.neo4j;

import org.junit.After;
import org.junit.Test;
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.test.EnvironmentTestUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.data.neo4j.server.Neo4jServer;

import static org.junit.Assert.assertEquals;

/**
 * Tests for {@link org.springframework.boot.autoconfigure.neo4j.Neo4jDataAutoConfiguration}.
 *
 * @author Dave Syer
 */
public class Neo4jAutoConfigurationTests {

	private AnnotationConfigApplicationContext context;

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void clientExists() {
		this.context = new AnnotationConfigApplicationContext(
				PropertyPlaceholderAutoConfiguration.class, Neo4jDataAutoConfiguration.class);
		assertEquals(1, this.context.getBeanNamesForType(Neo4jServer.class).length);
	}

	@SuppressWarnings("deprecation")
	@Test
	public void optionsAdded() {
		this.context = new AnnotationConfigApplicationContext();
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.data.neo4j.host:localhost");
		this.context.refresh();
		assertEquals("http://localhost:7474", this.context.getBean(Neo4jServer.class).url());
	}

	@SuppressWarnings("deprecation")
	@Test
	public void optionsAddedButNoHost() {
		this.context = new AnnotationConfigApplicationContext();
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.data.neo4j.url:https://localhost:7473");
		this.context.refresh();
		assertEquals("https://localhost:7473", this.context.getBean(Neo4jServer.class).url());
	}

}
