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

package org.springframework.boot.autoconfigure.data.neo4j;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.junit.jupiter.api.Test;
import org.neo4j.ogm.driver.NativeTypesNotAvailableException;
import org.neo4j.ogm.driver.NativeTypesNotSupportedException;
import org.neo4j.ogm.drivers.embedded.driver.EmbeddedDriver;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;
import org.neo4j.ogm.session.event.Event;
import org.neo4j.ogm.session.event.EventListener;
import org.neo4j.ogm.session.event.PersistenceEvent;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.data.neo4j.city.City;
import org.springframework.boot.autoconfigure.data.neo4j.country.Country;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.annotation.EnableBookmarkManagement;
import org.springframework.data.neo4j.bookmark.BookmarkManager;
import org.springframework.data.neo4j.mapping.Neo4jMappingContext;
import org.springframework.data.neo4j.transaction.Neo4jTransactionManager;
import org.springframework.data.neo4j.web.support.OpenSessionInViewInterceptor;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link Neo4jDataAutoConfiguration}. Tests should not use the embedded driver
 * as it requires the complete Neo4j-Kernel and server to function properly.
 *
 * @author Stephane Nicoll
 * @author Michael Hunger
 * @author Vince Bickers
 * @author Andy Wilkinson
 * @author Kazuki Shimizu
 * @author Michael Simons
 */
class Neo4jDataAutoConfigurationTests {

	private WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
			.withClassLoader(new FilteredClassLoader(EmbeddedDriver.class))
			.withUserConfiguration(TestConfiguration.class).withConfiguration(
					AutoConfigurations.of(Neo4jDataAutoConfiguration.class, TransactionAutoConfiguration.class));

	@Test
	void defaultConfiguration() {
		this.contextRunner.withPropertyValues("spring.data.neo4j.uri=http://localhost:8989").run((context) -> {
			assertThat(context).hasSingleBean(org.neo4j.ogm.config.Configuration.class);
			assertThat(context).hasSingleBean(SessionFactory.class);
			assertThat(context).hasSingleBean(Neo4jTransactionManager.class);
			assertThat(context).doesNotHaveBean(OpenSessionInViewInterceptor.class);
			assertThat(context).doesNotHaveBean(BookmarkManager.class);
		});
	}

	@Test
	void customNeo4jTransactionManagerUsingProperties() {
		this.contextRunner.withPropertyValues("spring.transaction.default-timeout=30",
				"spring.transaction.rollback-on-commit-failure:true").run((context) -> {
					Neo4jTransactionManager transactionManager = context.getBean(Neo4jTransactionManager.class);
					assertThat(transactionManager.getDefaultTimeout()).isEqualTo(30);
					assertThat(transactionManager.isRollbackOnCommitFailure()).isTrue();
				});
	}

	@Test
	void customSessionFactory() {
		this.contextRunner.withUserConfiguration(CustomSessionFactory.class).run((context) -> {
			assertThat(context).doesNotHaveBean(org.neo4j.ogm.config.Configuration.class);
			assertThat(context).hasSingleBean(SessionFactory.class);
		});
	}

	@Test
	void customSessionFactoryShouldNotDisableOtherDefaults() {
		this.contextRunner.withUserConfiguration(CustomSessionFactory.class).run((context) -> {
			assertThat(context).hasSingleBean(SessionFactory.class);
			assertThat(context.getBean(SessionFactory.class)).isSameAs(context.getBean("customSessionFactory"));
			assertThat(context).hasSingleBean(Neo4jTransactionManager.class);
			assertThat(context).doesNotHaveBean(OpenSessionInViewInterceptor.class);
		});
	}

	@Test
	void customConfiguration() {
		this.contextRunner.withUserConfiguration(CustomConfiguration.class).run((context) -> {
			assertThat(context.getBean(org.neo4j.ogm.config.Configuration.class))
					.isSameAs(context.getBean("myConfiguration"));
			assertThat(context).hasSingleBean(SessionFactory.class);
			assertThat(context).hasSingleBean(org.neo4j.ogm.config.Configuration.class);
		});
	}

	@Test
	void usesAutoConfigurationPackageToPickUpDomainTypes() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.setClassLoader(new FilteredClassLoader(EmbeddedDriver.class));
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
	void openSessionInViewInterceptorCanBeEnabled() {
		this.contextRunner.withPropertyValues("spring.data.neo4j.open-in-view:true")
				.run((context) -> assertThat(context).hasSingleBean(OpenSessionInViewInterceptor.class));
	}

	@Test
	void shouldBeAbleToUseNativeTypesWithBolt() {
		this.contextRunner
				.withPropertyValues("spring.data.neo4j.uri=bolt://localhost:7687",
						"spring.data.neo4j.use-native-types:true")
				.withConfiguration(
						AutoConfigurations.of(Neo4jDataAutoConfiguration.class, TransactionAutoConfiguration.class))
				.run((context) -> assertThat(context).getBean(org.neo4j.ogm.config.Configuration.class)
						.hasFieldOrPropertyWithValue("useNativeTypes", true));
	}

	@Test
	void shouldFailWhenNativeTypesAreNotAvailable() {
		this.contextRunner.withClassLoader(new FilteredClassLoader("org.neo4j.ogm.drivers.bolt.types"))
				.withPropertyValues("spring.data.neo4j.uri=bolt://localhost:7687",
						"spring.data.neo4j.use-native-types:true")
				.withConfiguration(
						AutoConfigurations.of(Neo4jDataAutoConfiguration.class, TransactionAutoConfiguration.class))
				.run((context) -> {
					assertThat(context).hasFailed();
					assertThat(context.getStartupFailure())
							.hasRootCauseInstanceOf(NativeTypesNotAvailableException.class);
				});
	}

	@Test
	void shouldFailWhenNativeTypesAreNotSupported() {
		this.contextRunner
				.withPropertyValues("spring.data.neo4j.uri=http://localhost:7474",
						"spring.data.neo4j.use-native-types:true")
				.withConfiguration(
						AutoConfigurations.of(Neo4jDataAutoConfiguration.class, TransactionAutoConfiguration.class))
				.run((context) -> {
					assertThat(context).hasFailed();
					assertThat(context.getStartupFailure())
							.hasRootCauseInstanceOf(NativeTypesNotSupportedException.class);
				});
	}

	@Test
	void eventListenersAreAutoRegistered() {
		this.contextRunner.withUserConfiguration(EventListenerConfiguration.class).run((context) -> {
			Session session = context.getBean(SessionFactory.class).openSession();
			session.notifyListeners(new PersistenceEvent(null, Event.TYPE.PRE_SAVE));
			verify(context.getBean("eventListenerOne", EventListener.class)).onPreSave(any(Event.class));
			verify(context.getBean("eventListenerTwo", EventListener.class)).onPreSave(any(Event.class));
		});
	}

	@Test
	void providesARequestScopedBookmarkManagerIfNecessaryAndPossible() {
		this.contextRunner.withUserConfiguration(BookmarkManagementEnabledConfiguration.class).run((context) -> {
			BeanDefinition bookmarkManagerBean = context.getBeanFactory()
					.getBeanDefinition("scopedTarget.bookmarkManager");
			assertThat(bookmarkManagerBean.getScope()).isEqualTo(WebApplicationContext.SCOPE_REQUEST);
		});
	}

	@Test
	void providesASingletonScopedBookmarkManagerIfNecessaryAndPossible() {
		new ApplicationContextRunner().withClassLoader(new FilteredClassLoader(EmbeddedDriver.class))
				.withUserConfiguration(TestConfiguration.class, BookmarkManagementEnabledConfiguration.class)
				.withConfiguration(
						AutoConfigurations.of(Neo4jDataAutoConfiguration.class, TransactionAutoConfiguration.class))
				.run((context) -> {
					assertThat(context).hasSingleBean(BookmarkManager.class);
					assertThat(context.getBeanDefinitionNames()).doesNotContain("scopedTarget.bookmarkManager");
				});
	}

	@Test
	void doesNotProvideABookmarkManagerIfNotPossible() {
		this.contextRunner.withClassLoader(new FilteredClassLoader(Caffeine.class, EmbeddedDriver.class))
				.withUserConfiguration(BookmarkManagementEnabledConfiguration.class)
				.run((context) -> assertThat(context).doesNotHaveBean(BookmarkManager.class));
	}

	private static void assertDomainTypesDiscovered(Neo4jMappingContext mappingContext, Class<?>... types) {
		for (Class<?> type : types) {
			assertThat(mappingContext.getPersistentEntity(type)).isNotNull();
		}
	}

	@Configuration(proxyBeanMethods = false)
	@EntityScan(basePackageClasses = Country.class)
	static class TestConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomSessionFactory {

		@Bean
		SessionFactory customSessionFactory() {
			return mock(SessionFactory.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomConfiguration {

		@Bean
		org.neo4j.ogm.config.Configuration myConfiguration() {
			return new org.neo4j.ogm.config.Configuration.Builder().uri("http://localhost:12345").build();
		}

	}

	@Configuration(proxyBeanMethods = false)
	@EnableBookmarkManagement
	static class BookmarkManagementEnabledConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	static class EventListenerConfiguration {

		@Bean
		EventListener eventListenerOne() {
			return mock(EventListener.class);
		}

		@Bean
		EventListener eventListenerTwo() {
			return mock(EventListener.class);
		}

	}

}
