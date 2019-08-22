/*
 * Copyright 2012-2019 the original author or authors.
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

import java.util.Base64;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.neo4j.ogm.config.AutoIndexMode;
import org.neo4j.ogm.config.Configuration;
import org.neo4j.ogm.config.Credentials;
import org.neo4j.ogm.drivers.embedded.driver.EmbeddedDriver;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link Neo4jProperties}.
 *
 * @author Stephane Nicoll
 * @author Michael Simons
 */
class Neo4jPropertiesTests {

	private AnnotationConfigApplicationContext context;

	@AfterEach
	void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	void defaultUseEmbeddedInMemoryIfAvailable() {
		Neo4jProperties properties = load(true);
		Configuration configuration = properties.createConfiguration();
		assertDriver(configuration, Neo4jProperties.EMBEDDED_DRIVER, null);
	}

	@Test
	void defaultUseBoltDriverIfEmbeddedDriverIsNotAvailable() {
		Neo4jProperties properties = load(false);
		Configuration configuration = properties.createConfiguration();
		assertDriver(configuration, Neo4jProperties.BOLT_DRIVER, Neo4jProperties.DEFAULT_BOLT_URI);
	}

	@Test
	void httpUriUseHttpDriver() {
		Neo4jProperties properties = load(true, "spring.data.neo4j.uri=http://localhost:7474");
		Configuration configuration = properties.createConfiguration();
		assertDriver(configuration, Neo4jProperties.HTTP_DRIVER, "http://localhost:7474");
	}

	@Test
	void httpsUriUseHttpDriver() {
		Neo4jProperties properties = load(true, "spring.data.neo4j.uri=https://localhost:7474");
		Configuration configuration = properties.createConfiguration();
		assertDriver(configuration, Neo4jProperties.HTTP_DRIVER, "https://localhost:7474");
	}

	@Test
	void boltUriUseBoltDriver() {
		Neo4jProperties properties = load(true, "spring.data.neo4j.uri=bolt://localhost:7687");
		Configuration configuration = properties.createConfiguration();
		assertDriver(configuration, Neo4jProperties.BOLT_DRIVER, "bolt://localhost:7687");
	}

	@Test
	void fileUriUseEmbeddedServer() {
		Neo4jProperties properties = load(true, "spring.data.neo4j.uri=file://var/tmp/graph.db");
		Configuration configuration = properties.createConfiguration();
		assertDriver(configuration, Neo4jProperties.EMBEDDED_DRIVER, "file://var/tmp/graph.db");
	}

	@Test
	void credentialsAreSet() {
		Neo4jProperties properties = load(true, "spring.data.neo4j.uri=http://localhost:7474",
				"spring.data.neo4j.username=user", "spring.data.neo4j.password=secret");
		Configuration configuration = properties.createConfiguration();
		assertDriver(configuration, Neo4jProperties.HTTP_DRIVER, "http://localhost:7474");
		assertCredentials(configuration, "user", "secret");
	}

	@Test
	void credentialsAreSetFromUri() {
		Neo4jProperties properties = load(true, "spring.data.neo4j.uri=https://user:secret@my-server:7474");
		Configuration configuration = properties.createConfiguration();
		assertDriver(configuration, Neo4jProperties.HTTP_DRIVER, "https://my-server:7474");
		assertCredentials(configuration, "user", "secret");
	}

	@Test
	void autoIndexNoneByDefault() {
		Neo4jProperties properties = load(true);
		Configuration configuration = properties.createConfiguration();
		assertThat(configuration.getAutoIndex()).isEqualTo(AutoIndexMode.NONE);
	}

	@Test
	void autoIndexCanBeConfigured() {
		Neo4jProperties properties = load(true, "spring.data.neo4j.auto-index=validate");
		Configuration configuration = properties.createConfiguration();
		assertThat(configuration.getAutoIndex()).isEqualTo(AutoIndexMode.VALIDATE);
	}

	@Test
	void embeddedModeDisabledUseBoltUri() {
		Neo4jProperties properties = load(true, "spring.data.neo4j.embedded.enabled=false");
		Configuration configuration = properties.createConfiguration();
		assertDriver(configuration, Neo4jProperties.BOLT_DRIVER, Neo4jProperties.DEFAULT_BOLT_URI);
	}

	@Test
	void embeddedModeWithRelativeLocation() {
		Neo4jProperties properties = load(true, "spring.data.neo4j.uri=file:relative/path/to/my.db");
		Configuration configuration = properties.createConfiguration();
		assertDriver(configuration, Neo4jProperties.EMBEDDED_DRIVER, "file:relative/path/to/my.db");
	}

	@Test
	void nativeTypesAreSetToFalseByDefault() {
		Neo4jProperties properties = load(true);
		Configuration configuration = properties.createConfiguration();
		assertThat(configuration.getUseNativeTypes()).isFalse();
	}

	@Test
	void nativeTypesCanBeConfigured() {
		Neo4jProperties properties = load(true, "spring.data.neo4j.use-native-types=true");
		Configuration configuration = properties.createConfiguration();
		assertThat(configuration.getUseNativeTypes()).isTrue();
	}

	private static void assertDriver(Configuration actual, String driver, String uri) {
		assertThat(actual).isNotNull();
		assertThat(actual.getDriverClassName()).isEqualTo(driver);
		assertThat(actual.getURI()).isEqualTo(uri);
	}

	private static void assertCredentials(Configuration actual, String username, String password) {
		Credentials<?> credentials = actual.getCredentials();
		if (username == null && password == null) {
			assertThat(credentials).isNull();
		}
		else {
			assertThat(credentials).isNotNull();
			Object content = credentials.credentials();
			assertThat(content).isInstanceOf(String.class);
			String[] auth = new String(Base64.getDecoder().decode((String) content)).split(":");
			assertThat(auth).containsExactly(username, password);
		}
	}

	Neo4jProperties load(boolean embeddedAvailable, String... environment) {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		if (!embeddedAvailable) {
			ctx.setClassLoader(new FilteredClassLoader(EmbeddedDriver.class));
		}
		TestPropertyValues.of(environment).applyTo(ctx);
		ctx.register(TestConfiguration.class);
		ctx.refresh();
		this.context = ctx;
		return this.context.getBean(Neo4jProperties.class);
	}

	@org.springframework.context.annotation.Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties(Neo4jProperties.class)
	static class TestConfiguration {

	}

}
