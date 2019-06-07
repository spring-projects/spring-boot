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

import org.junit.Test;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;
import org.neo4j.ogm.session.event.Event;
import org.neo4j.ogm.session.event.EventListener;
import org.neo4j.ogm.session.event.PersistenceEvent;

import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.data.neo4j.city.City;
import org.springframework.boot.autoconfigure.data.neo4j.country.Country;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.mapping.Neo4jMappingContext;
import org.springframework.data.neo4j.transaction.Neo4jTransactionManager;
import org.springframework.data.neo4j.web.support.OpenSessionInViewInterceptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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
public class Neo4jDataAutoConfigurationTests {

	private WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
			.withUserConfiguration(TestConfiguration.class).withConfiguration(
					AutoConfigurations.of(Neo4jDataAutoConfiguration.class, TransactionAutoConfiguration.class));

	@Test
	public void defaultConfiguration() {
		this.contextRunner.withPropertyValues("spring.data.neo4j.uri=http://localhost:8989").run((context) -> {
			assertThat(context).hasSingleBean(org.neo4j.ogm.config.Configuration.class);
			assertThat(context).hasSingleBean(SessionFactory.class);
			assertThat(context).hasSingleBean(Neo4jTransactionManager.class);
			assertThat(context).hasSingleBean(OpenSessionInViewInterceptor.class);
		});
	}

	@Test
	public void customNeo4jTransactionManagerUsingProperties() {
		this.contextRunner.withPropertyValues("spring.transaction.default-timeout=30",
				"spring.transaction.rollback-on-commit-failure:true").run((context) -> {
					Neo4jTransactionManager transactionManager = context.getBean(Neo4jTransactionManager.class);
					assertThat(transactionManager.getDefaultTimeout()).isEqualTo(30);
					assertThat(transactionManager.isRollbackOnCommitFailure()).isTrue();
				});
	}

	@Test
	public void customSessionFactory() {
		this.contextRunner.withUserConfiguration(CustomSessionFactory.class).run((context) -> {
			assertThat(context).doesNotHaveBean(org.neo4j.ogm.config.Configuration.class);
			assertThat(context).hasSingleBean(SessionFactory.class);
		});
	}

	@Test
	public void customConfiguration() {
		this.contextRunner.withUserConfiguration(CustomConfiguration.class).run((context) -> {
			assertThat(context.getBean(org.neo4j.ogm.config.Configuration.class))
					.isSameAs(context.getBean("myConfiguration"));
			assertThat(context).hasSingleBean(SessionFactory.class);
			assertThat(context).hasSingleBean(org.neo4j.ogm.config.Configuration.class);
		});

	}

	@Test
	public void usesAutoConfigurationPackageToPickUpDomainTypes() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		String cityPackage = City.class.getPackage().getName();
		AutoConfigurationPackages.register(context, cityPackage);
		context.register(Neo4jDataAutoConfiguration.class, Neo4jRepositoriesAutoConfiguration.class);
		try {
			context.refresh();
			assertDomainTypesDiscovered(context.getBean(Neo4jMappingContext.class), City.class);
		}
		finally {
			context.close();
		}
	}

	@Test
	public void openSessionInViewInterceptorCanBeDisabled() {
		this.contextRunner.withPropertyValues("spring.data.neo4j.open-in-view:false")
				.run((context) -> assertThat(context).doesNotHaveBean(OpenSessionInViewInterceptor.class));
	}

	@Test
	public void eventListenersAreAutoRegistered() {
		this.contextRunner.withUserConfiguration(EventListenerConfiguration.class).run((context) -> {
			Session session = context.getBean(SessionFactory.class).openSession();
			session.notifyListeners(new PersistenceEvent(null, Event.TYPE.PRE_SAVE));
			verify(context.getBean("eventListenerOne", EventListener.class)).onPreSave(any(Event.class));
			verify(context.getBean("eventListenerTwo", EventListener.class)).onPreSave(any(Event.class));
		});
	}

	private static void assertDomainTypesDiscovered(Neo4jMappingContext mappingContext, Class<?>... types) {
		for (Class<?> type : types) {
			assertThat(mappingContext.getPersistentEntity(type)).isNotNull();
		}
	}

	@Configuration
	@EntityScan(basePackageClasses = Country.class)
	static class TestConfiguration {

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
			return new org.neo4j.ogm.config.Configuration.Builder().uri("http://localhost:12345").build();
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
