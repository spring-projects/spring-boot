/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.autoconfigure.data.neo4j;

import java.net.URL;
import java.net.URLClassLoader;

import com.hazelcast.util.Base64;
import org.junit.After;
import org.junit.Test;
import org.neo4j.ogm.authentication.Credentials;
import org.neo4j.ogm.config.Configuration;
import org.neo4j.ogm.config.DriverConfiguration;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.util.EnvironmentTestUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link Neo4jProperties}.
 *
 * @author Stephane Nicoll
 */
public class Neo4jPropertiesTests {

	private AnnotationConfigApplicationContext context;

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void defaultUseEmbeddedInMemoryIfAvailable() {
		Neo4jProperties properties = load(true);
		Configuration configuration = properties.createConfiguration();
		assertDriver(configuration, Neo4jProperties.EMBEDDED_DRIVER, null);
	}

	@Test
	public void defaultUseHttpDriverIfEmbeddedDriverIsNotAvailable() {
		Neo4jProperties properties = load(false);
		Configuration configuration = properties.createConfiguration();
		assertDriver(configuration, Neo4jProperties.HTTP_DRIVER,
				Neo4jProperties.DEFAULT_HTTP_URI);
	}

	@Test
	public void httpUriUseHttpDriver() {
		Neo4jProperties properties = load(true,
				"spring.data.neo4j.uri=http://localhost:7474");
		Configuration configuration = properties.createConfiguration();
		assertDriver(configuration, Neo4jProperties.HTTP_DRIVER, "http://localhost:7474");
	}

	@Test
	public void httpsUriUseHttpDriver() {
		Neo4jProperties properties = load(true,
				"spring.data.neo4j.uri=https://localhost:7474");
		Configuration configuration = properties.createConfiguration();
		assertDriver(configuration, Neo4jProperties.HTTP_DRIVER,
				"https://localhost:7474");
	}

	@Test
	public void boltUriUseBoltDriver() {
		Neo4jProperties properties = load(true,
				"spring.data.neo4j.uri=bolt://localhost:7687");
		Configuration configuration = properties.createConfiguration();
		assertDriver(configuration, Neo4jProperties.BOLT_DRIVER, "bolt://localhost:7687");
	}

	@Test
	public void fileUriUseEmbeddedServer() {
		Neo4jProperties properties = load(true,
				"spring.data.neo4j.uri=file://var/tmp/graph.db");
		Configuration configuration = properties.createConfiguration();
		assertDriver(configuration, Neo4jProperties.EMBEDDED_DRIVER,
				"file://var/tmp/graph.db");
	}

	@Test
	public void credentialsAreSet() {
		Neo4jProperties properties = load(true,
				"spring.data.neo4j.uri=http://localhost:7474",
				"spring.data.neo4j.username=user", "spring.data.neo4j.password=secret");
		Configuration configuration = properties.createConfiguration();
		assertDriver(configuration, Neo4jProperties.HTTP_DRIVER, "http://localhost:7474");
		assertCredentials(configuration, "user", "secret");
	}

	@Test
	public void credentialsAreSetFromUri() {
		Neo4jProperties properties = load(true,
				"spring.data.neo4j.uri=http://user:secret@my-server:7474");
		Configuration configuration = properties.createConfiguration();
		assertDriver(configuration, Neo4jProperties.HTTP_DRIVER, "http://my-server:7474");
		assertCredentials(configuration, "user", "secret");
	}

	@Test
	public void embeddedModeDisabledUseHttpUri() {
		Neo4jProperties properties = load(true,
				"spring.data.neo4j.embedded.enabled=false");
		Configuration configuration = properties.createConfiguration();
		assertDriver(configuration, Neo4jProperties.HTTP_DRIVER,
				Neo4jProperties.DEFAULT_HTTP_URI);
	}

	@Test
	public void embeddedModeWithRelativeLocation() {
		Neo4jProperties properties = load(true,
				"spring.data.neo4j.uri=target/neo4j/my.db");
		Configuration configuration = properties.createConfiguration();
		assertDriver(configuration, Neo4jProperties.EMBEDDED_DRIVER,
				"target/neo4j/my.db");
	}

	private static void assertDriver(Configuration actual, String driver, String uri) {
		assertThat(actual).isNotNull();
		DriverConfiguration driverConfig = actual.driverConfiguration();
		assertThat(driverConfig.getDriverClassName()).isEqualTo(driver);
		assertThat(driverConfig.getURI()).isEqualTo(uri);
	}

	private static void assertCredentials(Configuration actual, String username,
			String password) {
		Credentials<?> credentials = actual.driverConfiguration().getCredentials();
		if (username == null & password == null) {
			assertThat(credentials).isNull();
		}
		else {
			assertThat(credentials).isNotNull();
			Object content = credentials.credentials();
			assertThat(content).isInstanceOf(String.class);
			String[] auth = new String(Base64.decode(((String) content).getBytes()))
					.split(":");
			assertThat(auth[0]).isEqualTo(username);
			assertThat(auth[1]).isEqualTo(password);
			assertThat(auth).hasSize(2);
		}
	}

	public Neo4jProperties load(final boolean embeddedAvailable, String... environment) {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.setClassLoader(new URLClassLoader(new URL[0], getClass().getClassLoader()) {

			@Override
			protected Class<?> loadClass(String name, boolean resolve)
					throws ClassNotFoundException {
				if (name.equals(Neo4jProperties.EMBEDDED_DRIVER)) {
					if (embeddedAvailable) {
						return TestEmbeddedDriver.class;
					}
					else {
						throw new ClassNotFoundException();
					}
				}
				return super.loadClass(name, resolve);
			}

		});
		EnvironmentTestUtils.addEnvironment(ctx, environment);
		ctx.register(TestConfiguration.class);
		ctx.refresh();
		this.context = ctx;
		return this.context.getBean(Neo4jProperties.class);
	}

	@org.springframework.context.annotation.Configuration
	@EnableConfigurationProperties(Neo4jProperties.class)
	static class TestConfiguration {

	}

	private static class TestEmbeddedDriver {

	}

}
