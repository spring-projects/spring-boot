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

package org.springframework.boot.actuate.autoconfigure.metrics.orm.jpa;

import java.util.HashMap;
import java.util.Map;

import javax.persistence.Entity;
import javax.persistence.EntityManagerFactory;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.PersistenceException;
import javax.sql.DataSource;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.hibernate.SessionFactory;
import org.junit.Test;
import org.mockito.ArgumentMatchers;

import org.springframework.boot.actuate.autoconfigure.metrics.test.MetricsRun;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link HibernateMetricsAutoConfiguration}.
 *
 * @author Rui Figueira
 * @author Stephane Nicoll
 */
public class HibernateMetricsAutoConfigurationTests {

	private ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.with(MetricsRun.simple())
			.withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class,
					HibernateJpaAutoConfiguration.class,
					HibernateMetricsAutoConfiguration.class))
			.withUserConfiguration(BaseConfiguration.class);

	@Test
	public void autoConfiguredEntityManagerFactoryWithStatsIsInstrumented() {
		this.contextRunner
				.withPropertyValues(
						"spring.jpa.properties.hibernate.generate_statistics:true")
				.run((context) -> {
					MeterRegistry registry = context.getBean(MeterRegistry.class);
					registry.get("hibernate.statements")
							.tags("entityManagerFactory", "entityManagerFactory").meter();
				});
	}

	@Test
	public void autoConfiguredEntityManagerFactoryWithoutStatsIsNotInstrumented() {
		this.contextRunner
				.withPropertyValues(
						"spring.jpa.properties.hibernate.generate_statistics:false")
				.run((context) -> {
					context.getBean(EntityManagerFactory.class)
							.unwrap(SessionFactory.class);
					MeterRegistry registry = context.getBean(MeterRegistry.class);
					assertThat(registry.find("hibernate.statements").meter()).isNull();
				});
	}

	@Test
	public void entityManagerFactoryInstrumentationCanBeDisabled() {
		this.contextRunner
				.withPropertyValues("management.metrics.enable.hibernate=false",
						"spring.jpa.properties.hibernate.generate_statistics:true")
				.run((context) -> {
					context.getBean(EntityManagerFactory.class)
							.unwrap(SessionFactory.class);
					MeterRegistry registry = context.getBean(MeterRegistry.class);
					assertThat(registry.find("hibernate.statements").meter()).isNull();
				});
	}

	@Test
	public void allEntityManagerFactoriesCanBeInstrumented() {
		this.contextRunner
				.withPropertyValues(
						"spring.jpa.properties.hibernate.generate_statistics:true")
				.withUserConfiguration(TwoEntityManagerFactoriesConfiguration.class)
				.run((context) -> {
					context.getBean("firstEntityManagerFactory",
							EntityManagerFactory.class).unwrap(SessionFactory.class);
					context.getBean("secondOne", EntityManagerFactory.class)
							.unwrap(SessionFactory.class);
					MeterRegistry registry = context.getBean(MeterRegistry.class);
					registry.get("hibernate.statements")
							.tags("entityManagerFactory", "first").meter();
					registry.get("hibernate.statements")
							.tags("entityManagerFactory", "secondOne").meter();
				});
	}

	@Test
	public void entityManagerFactoryInstrumentationIsDisabledIfNotHibernateSessionFactory() {
		this.contextRunner
				.withPropertyValues(
						"spring.jpa.properties.hibernate.generate_statistics:true")
				.withUserConfiguration(
						NonHibernateEntityManagerFactoryConfiguration.class)
				.run((context) -> {
					// ensure EntityManagerFactory is not a Hibernate SessionFactory
					assertThatThrownBy(() -> context.getBean(EntityManagerFactory.class)
							.unwrap(SessionFactory.class))
									.isInstanceOf(PersistenceException.class);
					MeterRegistry registry = context.getBean(MeterRegistry.class);
					assertThat(registry.find("hibernate.statements").meter()).isNull();
				});
	}

	@Test
	public void entityManagerFactoryInstrumentationIsDisabledIfHibernateIsNotAvailable() {
		this.contextRunner.withClassLoader(new FilteredClassLoader(SessionFactory.class))
				.withUserConfiguration(
						NonHibernateEntityManagerFactoryConfiguration.class)
				.run((context) -> {
					assertThat(context)
							.doesNotHaveBean(HibernateMetricsAutoConfiguration.class);
					MeterRegistry registry = context.getBean(MeterRegistry.class);
					assertThat(registry.find("hibernate.statements").meter()).isNull();
				});
	}

	@Configuration(proxyBeanMethods = false)
	static class BaseConfiguration {

		@Bean
		public SimpleMeterRegistry simpleMeterRegistry() {
			return new SimpleMeterRegistry();
		}

	}

	@Entity
	static class MyEntity {

		@Id
		@GeneratedValue
		private Long id;

	}

	@Configuration(proxyBeanMethods = false)
	static class TwoEntityManagerFactoriesConfiguration {

		private static final Class<?>[] PACKAGE_CLASSES = new Class<?>[] {
				MyEntity.class };

		@Primary
		@Bean
		public LocalContainerEntityManagerFactoryBean firstEntityManagerFactory(
				DataSource ds) {
			return createSessionFactory(ds);
		}

		@Bean
		public LocalContainerEntityManagerFactoryBean secondOne(DataSource ds) {
			return createSessionFactory(ds);
		}

		private LocalContainerEntityManagerFactoryBean createSessionFactory(
				DataSource ds) {
			Map<String, String> jpaProperties = new HashMap<>();
			jpaProperties.put("hibernate.generate_statistics", "true");
			return new EntityManagerFactoryBuilder(new HibernateJpaVendorAdapter(),
					jpaProperties, null).dataSource(ds).packages(PACKAGE_CLASSES).build();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class NonHibernateEntityManagerFactoryConfiguration {

		@Bean
		public EntityManagerFactory entityManagerFactory() {
			EntityManagerFactory mockedFactory = mock(EntityManagerFactory.class);
			// enforces JPA contract
			given(mockedFactory.unwrap(ArgumentMatchers.<Class<SessionFactory>>any()))
					.willThrow(PersistenceException.class);
			return mockedFactory;
		}

	}

}
