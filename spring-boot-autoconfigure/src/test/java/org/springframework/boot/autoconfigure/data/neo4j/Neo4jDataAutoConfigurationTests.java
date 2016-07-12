/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.autoconfigure.data.neo4j;

import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Test;
import org.neo4j.ogm.drivers.http.driver.HttpDriver;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;

import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.data.neo4j.city.City;
import org.springframework.boot.test.util.EnvironmentTestUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.mapping.Neo4jMappingContext;
import org.springframework.data.neo4j.template.Neo4jOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link Neo4jDataAutoConfiguration}. Tests can't use the embedded driver as we
 * use Lucene 4 and Neo4j still requires 3.
 *
 * @author Stephane Nicoll
 * @author Michael Hunger
 * @author Vince Bickers
 */
public class Neo4jDataAutoConfigurationTests {

	private AnnotationConfigApplicationContext context;

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void defaultConfiguration() {
		load(null, "spring.data.neo4j.uri=http://localhost:8989");
		assertThat(this.context.getBeansOfType(Neo4jOperations.class)).hasSize(1);
		assertThat(this.context.getBeansOfType(org.neo4j.ogm.config.Configuration.class))
				.hasSize(1);
		assertThat(this.context.getBeansOfType(SessionFactory.class)).hasSize(1);
		assertThat(this.context.getBeanDefinition("scopedTarget.getSession").getScope())
				.isEqualTo("singleton");
	}

	@Test
	public void customScope() {
		load(null, "spring.data.neo4j.uri=http://localhost:8989",
				"spring.data.neo4j.session.scope=prototype");
		assertThat(this.context.getBeanDefinition("scopedTarget.getSession").getScope())
				.isEqualTo("prototype");
	}

	@Test
	public void customNeo4jOperations() {
		load(CustomNeo4jOperations.class);
		assertThat(this.context.getBean(Neo4jOperations.class))
				.isSameAs(this.context.getBean("myNeo4jOperations"));
		assertThat(this.context.getBeansOfType(org.neo4j.ogm.config.Configuration.class))
				.hasSize(0);
		assertThat(this.context.getBeansOfType(SessionFactory.class)).hasSize(0);
		assertThat(this.context.getBeansOfType(Session.class)).hasSize(0);
	}

	@Test
	public void customConfiguration() {
		load(CustomConfiguration.class);
		assertThat(this.context.getBean(org.neo4j.ogm.config.Configuration.class))
				.isSameAs(this.context.getBean("myConfiguration"));
		assertThat(this.context.getBeansOfType(Neo4jOperations.class)).hasSize(1);
		assertThat(this.context.getBeansOfType(org.neo4j.ogm.config.Configuration.class))
				.hasSize(1);
		assertThat(this.context.getBeansOfType(SessionFactory.class)).hasSize(1);
	}

	@Test
	public void usesAutoConfigurationPackageToPickUpDomainTypes() {
		this.context = new AnnotationConfigApplicationContext();
		String cityPackage = City.class.getPackage().getName();
		AutoConfigurationPackages.register(this.context, cityPackage);
		this.context.register(Neo4jDataAutoConfiguration.class);
		this.context.refresh();
		assertDomainTypesDiscovered(this.context.getBean(Neo4jMappingContext.class),
				City.class);
	}

	private void load(Class<?> config, String... environment) {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		EnvironmentTestUtils.addEnvironment(ctx, environment);
		if (config != null) {
			ctx.register(config);
		}
		ctx.register(PropertyPlaceholderAutoConfiguration.class,
				Neo4jDataAutoConfiguration.class);
		ctx.refresh();
		this.context = ctx;
	}

	private static void assertDomainTypesDiscovered(Neo4jMappingContext mappingContext,
			Class<?>... types) {
		for (Class<?> type : types) {
			Assertions.assertThat(mappingContext.getPersistentEntity(type)).isNotNull();
		}
	}

	@Configuration
	static class CustomNeo4jOperations {

		@Bean
		public Neo4jOperations myNeo4jOperations() {
			return mock(Neo4jOperations.class);
		}

	}

	@Configuration
	static class CustomConfiguration {

		@Bean
		public org.neo4j.ogm.config.Configuration myConfiguration() {
			org.neo4j.ogm.config.Configuration configuration = new org.neo4j.ogm.config.Configuration();
			configuration.driverConfiguration()
					.setDriverClassName(HttpDriver.class.getName());
			return configuration;
		}

	}

}
