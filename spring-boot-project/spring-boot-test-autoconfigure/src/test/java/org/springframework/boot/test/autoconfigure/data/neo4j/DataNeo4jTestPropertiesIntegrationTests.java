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

package org.springframework.boot.test.autoconfigure.data.neo4j;

import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.testcontainers.containers.Neo4jContainer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.boot.testsupport.testcontainers.SkippableContainer;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the {@link DataNeo4jTest#properties properties} attribute of
 * {@link DataNeo4jTest @DataNeo4jTest}.
 *
 * @author Artsiom Yudovin
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(initializers = DataNeo4jTestPropertiesIntegrationTests.Initializer.class)
@DataNeo4jTest(properties = "spring.profiles.active=test")
public class DataNeo4jTestPropertiesIntegrationTests {

	@ClassRule
	public static SkippableContainer<Neo4jContainer<?>> neo4j = new SkippableContainer<>(
			() -> new Neo4jContainer<>().withAdminPassword(null));

	@Autowired
	private Environment environment;

	@Test
	public void environmentWithNewProfile() {
		assertThat(this.environment.getActiveProfiles()).containsExactly("test");
	}

	static class Initializer
			implements ApplicationContextInitializer<ConfigurableApplicationContext> {

		@Override
		public void initialize(
				ConfigurableApplicationContext configurableApplicationContext) {
			TestPropertyValues
					.of("spring.data.neo4j.uri=" + neo4j.getContainer().getBoltUrl())
					.applyTo(configurableApplicationContext.getEnvironment());
		}

	}

}
