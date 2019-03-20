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

import org.junit.After;
import org.junit.Test;
import org.neo4j.ogm.session.SessionFactory;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.autoconfigure.TestAutoConfigurationPackage;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.data.alt.neo4j.CityNeo4jRepository;
import org.springframework.boot.autoconfigure.data.empty.EmptyDataPackage;
import org.springframework.boot.autoconfigure.data.neo4j.city.City;
import org.springframework.boot.autoconfigure.data.neo4j.city.CityRepository;
import org.springframework.boot.test.util.EnvironmentTestUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.mapping.Neo4jMappingContext;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link Neo4jRepositoriesAutoConfiguration}.
 *
 * @author Dave Syer
 * @author Oliver Gierke
 * @author Michael Hunger
 * @author Vince Bickers
 * @author Stephane Nicoll
 */
public class Neo4jRepositoriesAutoConfigurationTests {

	private AnnotationConfigApplicationContext context;

	@After
	public void close() {
		this.context.close();
	}

	@Test
	public void testDefaultRepositoryConfiguration() throws Exception {
		prepareApplicationContext(TestConfiguration.class);
		assertThat(this.context.getBean(CityRepository.class)).isNotNull();
		Neo4jMappingContext mappingContext = this.context
				.getBean(Neo4jMappingContext.class);
		assertThat(mappingContext.getPersistentEntity(City.class)).isNotNull();
	}

	@Test
	public void testNoRepositoryConfiguration() throws Exception {
		prepareApplicationContext(EmptyConfiguration.class);
		assertThat(this.context.getBean(SessionFactory.class)).isNotNull();
	}

	@Test
	public void doesNotTriggerDefaultRepositoryDetectionIfCustomized() {
		prepareApplicationContext(CustomizedConfiguration.class);
		assertThat(this.context.getBean(CityNeo4jRepository.class)).isNotNull();
	}

	@Test(expected = NoSuchBeanDefinitionException.class)
	public void autoConfigurationShouldNotKickInEvenIfManualConfigDidNotCreateAnyRepositories() {
		prepareApplicationContext(SortOfInvalidCustomConfiguration.class);
		this.context.getBean(CityRepository.class);
	}

	private void prepareApplicationContext(Class<?>... configurationClasses) {
		this.context = new AnnotationConfigApplicationContext();
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.data.neo4j.uri=http://localhost:9797");
		this.context.register(configurationClasses);
		this.context.register(Neo4jDataAutoConfiguration.class,
				Neo4jRepositoriesAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
	}

	@Configuration
	@TestAutoConfigurationPackage(City.class)
	protected static class TestConfiguration {

	}

	@Configuration
	@TestAutoConfigurationPackage(EmptyDataPackage.class)
	protected static class EmptyConfiguration {

	}

	@Configuration
	@TestAutoConfigurationPackage(Neo4jRepositoriesAutoConfigurationTests.class)
	@EnableNeo4jRepositories(basePackageClasses = CityNeo4jRepository.class)
	protected static class CustomizedConfiguration {

	}

	@Configuration
	// To not find any repositories
	@EnableNeo4jRepositories("foo.bar")
	@TestAutoConfigurationPackage(Neo4jRepositoriesAutoConfigurationTests.class)
	protected static class SortOfInvalidCustomConfiguration {

	}

}
