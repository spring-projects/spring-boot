/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.session.data.mongodb.autoconfigure;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.data.mongodb.autoconfigure.MongoDataAutoConfiguration;
import org.springframework.boot.mongodb.autoconfigure.MongoAutoConfiguration;
import org.springframework.boot.session.autoconfigure.AbstractSessionAutoConfigurationTests;
import org.springframework.boot.session.autoconfigure.SessionAutoConfiguration;
import org.springframework.boot.test.context.assertj.AssertableWebApplicationContext;
import org.springframework.boot.test.context.runner.ContextConsumer;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.testsupport.container.TestImage;
import org.springframework.boot.web.server.autoconfigure.ServerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.config.SessionRepositoryCustomizer;
import org.springframework.session.data.mongo.MongoIndexedSessionRepository;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the servlet support of {@link MongoSessionAutoConfiguration}.
 *
 * @author Andy Wilkinson
 */
@Testcontainers(disabledWithoutDocker = true)
class MongoSessionAutoConfigurationTests extends AbstractSessionAutoConfigurationTests {

	@Container
	static final MongoDBContainer mongoDb = TestImage.container(MongoDBContainer.class);

	@BeforeEach
	void prepareContextRunner() {
		this.contextRunner = new WebApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(MongoAutoConfiguration.class, MongoDataAutoConfiguration.class,
					SessionAutoConfiguration.class, MongoSessionAutoConfiguration.class))
			.withPropertyValues("spring.data.mongodb.uri=" + mongoDb.getReplicaSetUrl());
	}

	@Test
	void defaultConfig() {
		this.contextRunner.run(validateSpringSessionUsesMongo("sessions"));
	}

	@Test
	void defaultConfigWithCustomTimeout() {
		this.contextRunner.withPropertyValues("spring.session.timeout=1m")
			.run(validateSpringSessionUsesMongo("sessions", Duration.ofMinutes(1)));
	}

	@Test
	void mongoSessionStoreWithCustomizations() {
		this.contextRunner.withPropertyValues("spring.session.mongodb.collection-name=foo")
			.run(validateSpringSessionUsesMongo("foo"));
	}

	@Test
	void whenTheUserDefinesTheirOwnSessionRepositoryCustomizerThenDefaultConfigurationIsOverwritten() {
		this.contextRunner.withUserConfiguration(CustomizerConfiguration.class)
			.withPropertyValues("spring.session.mongodb.collection-name=foo")
			.run(validateSpringSessionUsesMongo("customized"));
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
			assertThat(repository).hasFieldOrPropertyWithValue("defaultMaxInactiveInterval", timeout);
		};
	}

	@Configuration(proxyBeanMethods = false)
	static class CustomizerConfiguration {

		@Bean
		SessionRepositoryCustomizer<MongoIndexedSessionRepository> sessionRepositoryCustomizer() {
			return (repository) -> repository.setCollectionName("customized");
		}

	}

}
