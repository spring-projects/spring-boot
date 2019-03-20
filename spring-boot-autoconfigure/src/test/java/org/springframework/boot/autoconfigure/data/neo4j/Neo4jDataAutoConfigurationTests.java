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

import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Test;
import org.neo4j.ogm.drivers.http.driver.HttpDriver;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;
import org.neo4j.ogm.session.event.Event;
import org.neo4j.ogm.session.event.EventListener;
import org.neo4j.ogm.session.event.PersistenceEvent;

import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.data.neo4j.city.City;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.boot.test.util.EnvironmentTestUtils;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.mapping.Neo4jMappingContext;
import org.springframework.data.neo4j.template.Neo4jOperations;
import org.springframework.data.neo4j.transaction.Neo4jTransactionManager;
import org.springframework.data.neo4j.web.support.OpenSessionInViewInterceptor;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link Neo4jDataAutoConfiguration}. Tests can't use the embedded driver as we
 * use Lucene 4 and Neo4j still requires 3.
 *
 * @author Stephane Nicoll
 * @author Michael Hunger
 * @author Vince Bickers
 * @author Andy Wilkinson
 * @author Kazuki Shimizu
 */
@SuppressWarnings("deprecation")
public class Neo4jDataAutoConfigurationTests {

	private ConfigurableApplicationContext context;

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void defaultConfiguration() {
		load(null, "spring.data.neo4j.uri=http://localhost:8989");
		assertThat(this.context.getBeansOfType(org.neo4j.ogm.config.Configuration.class))
				.hasSize(1);
		assertThat(this.context.getBeansOfType(SessionFactory.class)).hasSize(1);
		assertThat(this.context.getBeansOfType(Neo4jOperations.class)).hasSize(1);
		assertThat(this.context.getBeansOfType(Neo4jTransactionManager.class)).hasSize(1);
		assertThat(this.context.getBeansOfType(OpenSessionInViewInterceptor.class))
				.hasSize(1);
	}

	@Test
	public void customNeo4jTransactionManagerUsingProperties() {
		load(null, "spring.transaction.default-timeout=30",
				"spring.transaction.rollback-on-commit-failure:true");
		Neo4jTransactionManager transactionManager = this.context
				.getBean(Neo4jTransactionManager.class);
		assertThat(transactionManager.getDefaultTimeout()).isEqualTo(30);
		assertThat(transactionManager.isRollbackOnCommitFailure()).isTrue();
	}

	@Test
	public void customSessionFactory() {
		load(CustomSessionFactory.class);
		assertThat(this.context.getBeansOfType(org.neo4j.ogm.config.Configuration.class))
				.hasSize(0);
		assertThat(this.context.getBeansOfType(SessionFactory.class)).hasSize(1);
	}

	@Test
	public void customConfiguration() {
		load(CustomConfiguration.class);
		assertThat(this.context.getBean(org.neo4j.ogm.config.Configuration.class))
				.isSameAs(this.context.getBean("myConfiguration"));
		assertThat(this.context.getBeansOfType(SessionFactory.class)).hasSize(1);
		assertThat(this.context.getBeansOfType(org.neo4j.ogm.config.Configuration.class))
				.hasSize(1);
	}

	@Test
	public void customNeo4jOperations() {
		load(CustomNeo4jOperations.class);
		assertThat(this.context.getBean(Neo4jOperations.class))
				.isSameAs(this.context.getBean("myNeo4jOperations"));
	}

	@Test
	public void usesAutoConfigurationPackageToPickUpDomainTypes() {
		this.context = new AnnotationConfigApplicationContext();
		String cityPackage = City.class.getPackage().getName();
		AutoConfigurationPackages.register((BeanDefinitionRegistry) this.context,
				cityPackage);
		((AnnotationConfigApplicationContext) this.context).register(
				Neo4jDataAutoConfiguration.class,
				Neo4jRepositoriesAutoConfiguration.class);
		this.context.refresh();
		assertDomainTypesDiscovered(this.context.getBean(Neo4jMappingContext.class),
				City.class);
	}

	@Test
	public void openSessionInViewInterceptorCanBeDisabled() {
		load(null, "spring.data.neo4j.open-in-view:false");
		assertThat(this.context.getBeansOfType(OpenSessionInViewInterceptor.class))
				.isEmpty();
	}

	@Test
	public void eventListenersAreAutoRegistered() {
		load(EventListenerConfiguration.class);
		Session session = this.context.getBean(SessionFactory.class).openSession();
		session.notifyListeners(new PersistenceEvent(null, Event.TYPE.PRE_SAVE));
		verify(this.context.getBean("eventListenerOne", EventListener.class))
				.onPreSave(any(Event.class));
		verify(this.context.getBean("eventListenerTwo", EventListener.class))
				.onPreSave(any(Event.class));
	}

	private void load(Class<?> config, String... environment) {
		AnnotationConfigWebApplicationContext ctx = new AnnotationConfigWebApplicationContext();
		EnvironmentTestUtils.addEnvironment(ctx, environment);
		if (config != null) {
			ctx.register(config);
		}
		ctx.register(PropertyPlaceholderAutoConfiguration.class,
				Neo4jDataAutoConfiguration.class, TransactionAutoConfiguration.class);
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
	static class CustomSessionFactory {

		@Bean
		public SessionFactory customSessionFactory() {
			return mock(SessionFactory.class);
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

	@Configuration
	static class CustomNeo4jOperations {

		@Bean
		public Neo4jOperations myNeo4jOperations() {
			return mock(Neo4jOperations.class);
		}

	}

	@Configuration
	static class EventListenerConfiguration {

		@Bean
		public EventListener eventListenerOne() {
			return mock(EventListener.class);
		}

		@Bean
		public EventListener eventListenerTwo() {
			return mock(EventListener.class);
		}

	}

}
