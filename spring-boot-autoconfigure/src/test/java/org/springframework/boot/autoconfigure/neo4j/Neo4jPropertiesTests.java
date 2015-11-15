/*
 * Copyright 2012-2015 the original author or authors.
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

import org.junit.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.EnvironmentTestUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.server.Neo4jServer;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Tests for {@link org.springframework.boot.autoconfigure.neo4j.Neo4jProperties}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
public class Neo4jPropertiesTests {

	@Test
	public void canBindCharArrayPassword() {
		// gh-1572
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		EnvironmentTestUtils.addEnvironment(context, "spring.data.neo4j.password:word");
		context.register(Conf.class);
		context.refresh();
		Neo4jProperties properties = context.getBean(Neo4jProperties.class);
		assertThat(properties.getPassword(), equalTo("word".toCharArray()));
	}

	@Test
	public void portCanBeCustomized() throws UnknownHostException, MalformedURLException {
		Neo4jProperties properties = new Neo4jProperties();
		properties.setPort(12345);
		Neo4jServer server = properties.createNeo4jServer(null);
		assertServerAddress(server, "localhost", 12345);
	}

	@Test
	public void hostCanBeCustomized() throws UnknownHostException, MalformedURLException {
		Neo4jProperties properties = new Neo4jProperties();
		properties.setHost("neo4j.example.com");
		Neo4jServer server = properties.createNeo4jServer(null);
		assertServerAddress(server, "neo4j.example.com", 7474);
	}

	@Test
	public void credentialsCanBeCustomized() throws UnknownHostException, MalformedURLException {
		Neo4jProperties properties = new Neo4jProperties();
		properties.setUsername("user");
		properties.setPassword("secret".toCharArray());
		Neo4jServer server = properties.createNeo4jServer(null);
		assertNeo4jCredential(server, "user", "secret");
	}

	@Test
	public void uriCanBeCustomized() throws UnknownHostException, MalformedURLException {
		Neo4jProperties properties = new Neo4jProperties();
		properties.setUrl("https://user:secret@neo4j1.example.com:12345");
		Neo4jServer server = properties.createNeo4jServer(null);
		assertServerAddress(server, "neo4j1.example.com", 12345);
		assertNeo4jCredential(server, "user", "secret");
	}

	private void assertServerAddress(Neo4jServer server, String expectedHost, int expectedPort) {
		try {
			URL url = new URL(server.url());
			assertThat(url.getHost(), equalTo(expectedHost));
			assertThat(url.getPort(), equalTo(expectedPort));
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
	}

	private void assertNeo4jCredential(Neo4jServer server,
									   String expectedUsername, String expectedPassword) {
		assertThat(server.username(), equalTo(expectedUsername));
		assertThat(server.password(), equalTo(expectedPassword));
	}

	@Configuration
	@EnableConfigurationProperties(Neo4jProperties.class)
	static class Conf {

	}

}
