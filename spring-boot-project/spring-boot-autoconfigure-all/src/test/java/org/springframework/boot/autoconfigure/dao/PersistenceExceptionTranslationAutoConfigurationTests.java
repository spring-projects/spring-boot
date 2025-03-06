/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.autoconfigure.dao;

import java.util.Map;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.jdbc.EmbeddedDataSourceConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.dao.annotation.PersistenceExceptionTranslationPostProcessor;
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
	void persistOfNullThrowsIllegalArgumentExceptionWithoutExceptionTranslation() {
		this.context = new AnnotationConfigApplicationContext(EmbeddedDataSourceConfiguration.class,
				HibernateJpaAutoConfiguration.class, TestConfiguration.class);
		assertThatIllegalArgumentException().isThrownBy(() -> this.context.getBean(TestRepository.class).doSomething());
	}

	@Test
	void persistOfNullThrowsInvalidDataAccessApiUsageExceptionWithExceptionTranslation() {
		this.context = new AnnotationConfigApplicationContext(EmbeddedDataSourceConfiguration.class,
				HibernateJpaAutoConfiguration.class, TestConfiguration.class,
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

}
