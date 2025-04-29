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

package org.springframework.boot.transaction.autoconfigure;

import java.io.Serializable;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariDataSource;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import org.hibernate.engine.transaction.jta.platform.internal.NoJtaPlatform;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.boot.testsupport.classpath.resources.WithResource;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.dao.annotation.PersistenceExceptionTranslationPostProcessor;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.stereotype.Repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link PersistenceExceptionTranslationAutoConfiguration}
 *
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 */
class PersistenceExceptionTranslationAutoConfigurationTests {

	private AnnotationConfigApplicationContext context;

	@AfterEach
	void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	void exceptionTranslationPostProcessorUsesCglibByDefault() {
		this.context = new AnnotationConfigApplicationContext(PersistenceExceptionTranslationAutoConfiguration.class);
		Map<String, PersistenceExceptionTranslationPostProcessor> beans = this.context
			.getBeansOfType(PersistenceExceptionTranslationPostProcessor.class);
		assertThat(beans).hasSize(1);
		assertThat(beans.values().iterator().next().isProxyTargetClass()).isTrue();
	}

	@Test
	void exceptionTranslationPostProcessorCanBeConfiguredToUseJdkProxy() {
		this.context = new AnnotationConfigApplicationContext();
		TestPropertyValues.of("spring.aop.proxy-target-class=false").applyTo(this.context);
		this.context.register(PersistenceExceptionTranslationAutoConfiguration.class);
		this.context.refresh();
		Map<String, PersistenceExceptionTranslationPostProcessor> beans = this.context
			.getBeansOfType(PersistenceExceptionTranslationPostProcessor.class);
		assertThat(beans).hasSize(1);
		assertThat(beans.values().iterator().next().isProxyTargetClass()).isFalse();
	}

	@Test
	void exceptionTranslationPostProcessorCanBeDisabled() {
		this.context = new AnnotationConfigApplicationContext();
		TestPropertyValues.of("spring.dao.exceptiontranslation.enabled=false").applyTo(this.context);
		this.context.register(PersistenceExceptionTranslationAutoConfiguration.class);
		this.context.refresh();
		Map<String, PersistenceExceptionTranslationPostProcessor> beans = this.context
			.getBeansOfType(PersistenceExceptionTranslationPostProcessor.class);
		assertThat(beans).isEmpty();
	}

	@Test
	@WithMetaInfPersistenceXmlResource
	void persistOfNullThrowsIllegalArgumentExceptionWithoutExceptionTranslation() {
		this.context = new AnnotationConfigApplicationContext(JpaConfiguration.class, TestConfiguration.class);
		assertThatIllegalArgumentException().isThrownBy(() -> this.context.getBean(TestRepository.class).doSomething());
	}

	@Test
	@WithMetaInfPersistenceXmlResource
	void persistOfNullThrowsInvalidDataAccessApiUsageExceptionWithExceptionTranslation() {
		this.context = new AnnotationConfigApplicationContext(JpaConfiguration.class, TestConfiguration.class,
				PersistenceExceptionTranslationAutoConfiguration.class);
		assertThatExceptionOfType(InvalidDataAccessApiUsageException.class)
			.isThrownBy(() -> this.context.getBean(TestRepository.class).doSomething());
	}

	@Configuration(proxyBeanMethods = false)
	static class TestConfiguration {

		@Bean
		TestRepository testRepository(EntityManagerFactory entityManagerFactory) {
			return new TestRepository(entityManagerFactory.createEntityManager());
		}

	}

	@Repository
	static class TestRepository {

		private final EntityManager entityManager;

		TestRepository(EntityManager entityManager) {
			this.entityManager = entityManager;
		}

		void doSomething() {
			this.entityManager.persist(null);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class JpaConfiguration {

		@Bean
		DataSource dataSource() {
			HikariDataSource dataSource = new HikariDataSource();
			dataSource.setDriverClassName("org.hsqldb.jdbc.JDBCDriver");
			dataSource.setJdbcUrl("jdbc:hsqldb:mem:tx");
			dataSource.setUsername("sa");
			return dataSource;
		}

		@Bean
		LocalContainerEntityManagerFactoryBean entityManagerFactoryBean(DataSource dataSource) {
			LocalContainerEntityManagerFactoryBean localContainerEntityManagerFactoryBean = new LocalContainerEntityManagerFactoryBean();
			localContainerEntityManagerFactoryBean.setDataSource(dataSource);
			localContainerEntityManagerFactoryBean.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
			localContainerEntityManagerFactoryBean.setJpaPropertyMap(configureJpaProperties());
			return localContainerEntityManagerFactoryBean;
		}

		private static Map<String, ?> configureJpaProperties() {
			Map<String, Object> properties = new HashMap<>();
			properties.put("configured", "manually");
			properties.put("hibernate.transaction.jta.platform", NoJtaPlatform.INSTANCE);
			return properties;
		}

	}

	@Target(ElementType.METHOD)
	@Retention(RetentionPolicy.RUNTIME)
	@WithResource(name = "META-INF/persistence.xml",
			content = """
					<?xml version="1.0" encoding="UTF-8"?>
					<persistence version="2.0" xmlns="http://java.sun.com/xml/ns/persistence" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://java.sun.com/xml/ns/persistence https://java.sun.com/xml/ns/persistence/persistence_2_0.xsd">
						<persistence-unit name="manually-configured">
							<class>org.springframework.boot.transaction.autoconfigure.PersistenceExceptionTranslationAutoConfigurationTests$City</class>
							<exclude-unlisted-classes>true</exclude-unlisted-classes>
						</persistence-unit>
					</persistence>
					""")
	@interface WithMetaInfPersistenceXmlResource {

	}

	@Entity
	public static class City implements Serializable {

		private static final long serialVersionUID = 1L;

		@Id
		@GeneratedValue
		private Long id;

		@Column(nullable = false)
		private String name;

		@Column(nullable = false)
		private String state;

		@Column(nullable = false)
		private String country;

		@Column(nullable = false)
		private String map;

		protected City() {
		}

		City(String name, String state, String country, String map) {
			this.name = name;
			this.state = state;
			this.country = country;
			this.map = map;
		}

		public String getName() {
			return this.name;
		}

		public String getState() {
			return this.state;
		}

		public String getCountry() {
			return this.country;
		}

		public String getMap() {
			return this.map;
		}

		@Override
		public String toString() {
			return getName() + "," + getState() + "," + getCountry();
		}

	}

}
