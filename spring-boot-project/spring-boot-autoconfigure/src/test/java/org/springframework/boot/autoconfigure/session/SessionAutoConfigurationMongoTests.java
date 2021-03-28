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

package org.springframework.boot.autoconfigure.session;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.assertj.AssertableWebApplicationContext;
import org.springframework.boot.test.context.runner.ContextConsumer;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.testsupport.testcontainers.DockerImageNames;
import org.springframework.session.data.mongo.MongoIndexedSessionRepository;
import org.springframework.session.data.redis.RedisIndexedSessionRepository;
import org.springframework.session.hazelcast.HazelcastIndexedSessionRepository;
import org.springframework.session.jdbc.JdbcIndexedSessionRepository;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Mongo-specific tests for {@link SessionAutoConfiguration}.
 *
 * @author Andy Wilkinson
 */
@Testcontainers(disabledWithoutDocker = true)
class SessionAutoConfigurationMongoTests extends AbstractSessionAutoConfigurationTests {

	@Container
	static final MongoDBContainer mongoDB = new MongoDBContainer(DockerImageNames.mongo()).withStartupAttempts(5)
			.withStartupTimeout(Duration.ofMinutes(5));

	private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(MongoAutoConfiguration.class, MongoDataAutoConfiguration.class,
					SessionAutoConfiguration.class))
			.withPropertyValues("spring.data.mongodb.uri=" + mongoDB.getReplicaSetUrl());

	@Test
	void defaultConfig() {
		this.contextRunner.withPropertyValues("spring.session.store-type=mongodb")
				.run(validateSpringSessionUsesMongo("sessions"));
	}

	@Test
	void defaultConfigWithUniqueStoreImplementation() {
		this.contextRunner
				.withClassLoader(new FilteredClassLoader(HazelcastIndexedSessionRepository.class,
						JdbcIndexedSessionRepository.class, RedisIndexedSessionRepository.class))
				.run(validateSpringSessionUsesMongo("sessions"));
	}

	@Test
	void defaultConfigWithCustomTimeout() {
		this.contextRunner.withPropertyValues("spring.session.store-type=mongodb", "spring.session.timeout=1m")
				.run(validateSpringSessionUsesMongo("sessions", Duration.ofMinutes(1)));
	}

	@Test
	void mongoSessionStoreWithCustomizations() {
		this.contextRunner
				.withPropertyValues("spring.session.store-type=mongodb", "spring.session.mongodb.collection-name=foo")
				.run(validateSpringSessionUsesMongo("foo"));
	}

	private ContextConsumer<AssertableWebApplicationContext> validateSpringSessionUsesMongo(String collectionName) {
		return validateSpringSessionUsesMongo(collectionName,
				new ServerProperties().getServlet().getSession().getTimeout());
	}

	private ContextConsumer<AssertableWebApplicationContext> validateSpringSessionUsesMongo(String collectionName,
			Duration timeout) {
		return (context) -> {
			MongoIndexedSessionRepository repository = validateSessionRepository(context,
					MongoIndexedSessionRepository.class);
			assertThat(repository).hasFieldOrPropertyWithValue("collectionName", collectionName);
			assertThat(repository).hasFieldOrPropertyWithValue("maxInactiveIntervalInSeconds",
					(int) timeout.getSeconds());
		};
	}

}
