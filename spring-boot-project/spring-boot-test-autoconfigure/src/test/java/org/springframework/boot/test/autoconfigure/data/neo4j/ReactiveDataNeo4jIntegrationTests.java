/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.test.autoconfigure.data.neo4j;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.data.neo4j.core.ReactiveNeo4jTemplate;
import org.springframework.data.neo4j.core.transaction.ReactiveNeo4jTransactionManager;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.ReactiveTransactionManager;
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;

import org.neo4j.driver.AccessMode;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.SessionConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Integration tests for the reactive SDN/RX Neo4j test slice.
 *
 * @author Michael J. Simons
 * @since 2.4.0
 */
@DataNeo4jTest
@Testcontainers(disabledWithoutDocker = true)
class ReactiveDataNeo4jIntegrationTests {

	@Container
	static final Neo4jContainer<?> neo4j = new Neo4jContainer<>("neo4j:4.0").withoutAuthentication()
			.withStartupTimeout(Duration.ofMinutes(10));

	@DynamicPropertySource
	static void neo4jProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.neo4j.uri", neo4j::getBoltUrl);
	}

	@Autowired
	private Driver driver;

	@Autowired
	private ReactiveNeo4jTemplate neo4jTemplate;

	@Autowired
	private ApplicationContext applicationContext;

	@Test
	void testTemplate() {

		Mono.just(new ExampleGraph("Look, new @DataNeo4jTest with reactive!")).flatMap(neo4jTemplate::save)
				.as(StepVerifier::create).expectNextCount(1).verifyComplete();

		try (Session session = driver.session(SessionConfig.builder().withDefaultAccessMode(AccessMode.READ).build())) {
			long cnt = session.run("MATCH (n:ExampleGraph) RETURN count(n) as cnt").single().get("cnt").asLong();
			assertThat(cnt).isEqualTo(1L);
		}
	}

	@Test
	void didNotInjectExampleService() {
		assertThatExceptionOfType(NoSuchBeanDefinitionException.class)
				.isThrownBy(() -> this.applicationContext.getBean(ExampleService.class));
	}

	@Test
	void didProvideOnlyReactiveTransactionManager() {

		assertThat(this.applicationContext.getBean(ReactiveTransactionManager.class))
				.isInstanceOf(ReactiveNeo4jTransactionManager.class);
		assertThatExceptionOfType(NoSuchBeanDefinitionException.class)
				.isThrownBy(() -> this.applicationContext.getBean(PlatformTransactionManager.class));
	}

}
