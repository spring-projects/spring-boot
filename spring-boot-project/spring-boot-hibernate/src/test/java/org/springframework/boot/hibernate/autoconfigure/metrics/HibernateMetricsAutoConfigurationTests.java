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

package org.springframework.boot.hibernate.autoconfigure.metrics;

import java.util.Map;
import java.util.function.Function;

import javax.sql.DataSource;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PersistenceException;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceInitializationAutoConfiguration;
import org.springframework.boot.jpa.EntityManagerFactoryBuilder;
import org.springframework.boot.jpa.autoconfigure.EntityManagerFactoryBuilderCustomizer;
import org.springframework.boot.metrics.autoconfigure.MetricsAutoConfiguration;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.testsupport.classpath.resources.WithResource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link HibernateMetricsAutoConfiguration}.
 *
 * @author Rui Figueira
 * @author Stephane Nicoll
 */
class HibernateMetricsAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withBean(SimpleMeterRegistry.class)
		.withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class,
				HibernateMetricsAutoConfiguration.class, MetricsAutoConfiguration.class))
		.withPropertyValues("management.metrics.use-global-registry=false");

	@Test
	void autoConfiguredEntityManagerFactoryWithStatsIsInstrumented() {
		this.contextRunner.withPropertyValues("spring.jpa.properties.hibernate.generate_statistics:true")
			.run((context) -> {
				MeterRegistry registry = context.getBean(MeterRegistry.class);
				registry.get("hibernate.statements").tags("entityManagerFactory", "entityManagerFactory").meter();
			});
	}

	@Test
	void autoConfiguredEntityManagerFactoryWithoutStatsIsNotInstrumented() {
		this.contextRunner.withPropertyValues("spring.jpa.properties.hibernate.generate_statistics:false")
			.run((context) -> {
				context.getBean(EntityManagerFactory.class).unwrap(SessionFactory.class);
				MeterRegistry registry = context.getBean(MeterRegistry.class);
				assertThat(registry.find("hibernate.statements").meter()).isNull();
			});
	}

	@Test
	void entityManagerFactoryInstrumentationCanBeDisabled() {
		this.contextRunner
			.withPropertyValues("management.metrics.enable.hibernate=false",
					"spring.jpa.properties.hibernate.generate_statistics:true")
			.run((context) -> {
				context.getBean(EntityManagerFactory.class).unwrap(SessionFactory.class);
				MeterRegistry registry = context.getBean(MeterRegistry.class);
				assertThat(registry.find("hibernate.statements").meter()).isNull();
			});
	}

	@Test
	void allEntityManagerFactoriesCanBeInstrumented() {
		this.contextRunner.withPropertyValues("spring.jpa.properties.hibernate.generate_statistics:true")
			.withUserConfiguration(MultipleEntityManagerFactoriesConfiguration.class)
			.run((context) -> {
				context.getBean("firstEntityManagerFactory", EntityManagerFactory.class).unwrap(SessionFactory.class);
				context.getBean("nonDefault", EntityManagerFactory.class).unwrap(SessionFactory.class);
				context.getBean("nonAutowire", EntityManagerFactory.class).unwrap(SessionFactory.class);
				MeterRegistry registry = context.getBean(MeterRegistry.class);
				assertThat(registry.find("hibernate.statements").meters())
					.map((meter) -> meter.getId().getTag("entityManagerFactory"))
					.containsOnly("first", "nonDefault");
			});
	}

	@Test
	void entityManagerFactoryInstrumentationIsDisabledIfNotHibernateSessionFactory() {
		this.contextRunner.withPropertyValues("spring.jpa.properties.hibernate.generate_statistics:true")
			.withUserConfiguration(NonHibernateEntityManagerFactoryConfiguration.class)
			.run((context) -> {
				// ensure EntityManagerFactory is not a Hibernate SessionFactory
				assertThatExceptionOfType(PersistenceException.class)
					.isThrownBy(() -> context.getBean(EntityManagerFactory.class).unwrap(SessionFactory.class));
				MeterRegistry registry = context.getBean(MeterRegistry.class);
				assertThat(registry.find("hibernate.statements").meter()).isNull();
			});
	}

	@Test
	void entityManagerFactoryInstrumentationIsDisabledIfHibernateIsNotAvailable() {
		this.contextRunner.withClassLoader(new FilteredClassLoader(SessionFactory.class))
			.withUserConfiguration(NonHibernateEntityManagerFactoryConfiguration.class)
			.run((context) -> {
				assertThat(context).doesNotHaveBean(HibernateMetricsAutoConfiguration.class);
				MeterRegistry registry = context.getBean(MeterRegistry.class);
				assertThat(registry.find("hibernate.statements").meter()).isNull();
			});
	}

	@Test
	@WithResource(name = "city-schema.sql", content = """
			CREATE TABLE CITY (
			  id INTEGER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
			  name VARCHAR(30),
			  state  VARCHAR(30),
			  country  VARCHAR(30),
			  map  VARCHAR(30)
			);
			""")
	@WithResource(name = "city-data.sql",
			content = "INSERT INTO CITY (ID, NAME, STATE, COUNTRY, MAP) values (2000, 'Washington', 'DC', 'US', 'Google');")
	void entityManagerFactoryInstrumentationDoesNotDeadlockWithDeferredInitialization() {
		this.contextRunner
			.withPropertyValues("spring.jpa.properties.hibernate.generate_statistics:true",
					"spring.sql.init.schema-locations:city-schema.sql", "spring.sql.init.data-locations=city-data.sql")
			.withConfiguration(AutoConfigurations.of(DataSourceInitializationAutoConfiguration.class))
			.withBean(EntityManagerFactoryBuilderCustomizer.class,
					() -> (builder) -> builder.setBootstrapExecutor(new SimpleAsyncTaskExecutor()))
			.run((context) -> {
				JdbcTemplate jdbcTemplate = new JdbcTemplate(context.getBean(DataSource.class));
				assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) from CITY", Integer.class)).isOne();
				MeterRegistry registry = context.getBean(MeterRegistry.class);
				registry.get("hibernate.statements").tags("entityManagerFactory", "entityManagerFactory").meter();
			});
	}

	@Configuration(proxyBeanMethods = false)
	static class BaseConfiguration {

		@Bean
		SimpleMeterRegistry simpleMeterRegistry() {
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
	static class MultipleEntityManagerFactoriesConfiguration {

		private static final Class<?>[] PACKAGE_CLASSES = new Class<?>[] { MyEntity.class };

		@Primary
		@Bean
		LocalContainerEntityManagerFactoryBean firstEntityManagerFactory(DataSource ds) {
			return createSessionFactory(ds);
		}

		@Bean(defaultCandidate = false)
		LocalContainerEntityManagerFactoryBean nonDefault(DataSource ds) {
			return createSessionFactory(ds);
		}

		@Bean(autowireCandidate = false)
		LocalContainerEntityManagerFactoryBean nonAutowire(DataSource ds) {
			return createSessionFactory(ds);
		}

		private LocalContainerEntityManagerFactoryBean createSessionFactory(DataSource ds) {
			Function<DataSource, Map<String, ?>> jpaPropertiesFactory = (dataSource) -> Map
				.of("hibernate.generate_statistics", "true");
			return new EntityManagerFactoryBuilder(new HibernateJpaVendorAdapter(), jpaPropertiesFactory, null)
				.dataSource(ds)
				.packages(PACKAGE_CLASSES)
				.build();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class NonHibernateEntityManagerFactoryConfiguration {

		@Bean
		EntityManagerFactory entityManagerFactory() {
			EntityManagerFactory mockedFactory = mock(EntityManagerFactory.class);
			// enforces JPA contract
			given(mockedFactory.unwrap(ArgumentMatchers.<Class<SessionFactory>>any()))
				.willThrow(PersistenceException.class);
			return mockedFactory;
		}

	}

}
