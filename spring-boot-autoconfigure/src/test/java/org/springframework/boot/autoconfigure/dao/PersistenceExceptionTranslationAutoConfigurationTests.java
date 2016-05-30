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

package org.springframework.boot.autoconfigure.dao;

import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.junit.After;
import org.junit.Test;

import org.springframework.boot.autoconfigure.jdbc.EmbeddedDataSourceConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.util.EnvironmentTestUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.dao.annotation.PersistenceExceptionTranslationPostProcessor;
import org.springframework.stereotype.Repository;

import static org.assertj.core.api.Assertions.assertThat;

/**
 *
 * Tests for {@link PersistenceExceptionTranslationAutoConfiguration}
 *
 * @author Andy Wilkinson
 */
public class PersistenceExceptionTranslationAutoConfigurationTests {

	private AnnotationConfigApplicationContext context;

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void exceptionTranslationPostProcessorBeanIsCreated() {
		this.context = new AnnotationConfigApplicationContext(
				PersistenceExceptionTranslationAutoConfiguration.class);
		Map<String, PersistenceExceptionTranslationPostProcessor> beans = this.context
				.getBeansOfType(PersistenceExceptionTranslationPostProcessor.class);
		assertThat(beans).hasSize(1);
		assertThat(beans.values().iterator().next().isProxyTargetClass()).isTrue();
	}

	@Test
	public void exceptionTranslationPostProcessorBeanIsDisabled() {
		this.context = new AnnotationConfigApplicationContext();
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.dao.exceptiontranslation.enabled=false");
		this.context.register(PersistenceExceptionTranslationAutoConfiguration.class);
		this.context.refresh();
		Map<String, PersistenceExceptionTranslationPostProcessor> beans = this.context
				.getBeansOfType(PersistenceExceptionTranslationPostProcessor.class);
		assertThat(beans.entrySet()).isEmpty();
	}

	@Test(expected = IllegalArgumentException.class)
	public void persistOfNullThrowsIllegalArgumentExceptionWithoutExceptionTranslation() {
		this.context = new AnnotationConfigApplicationContext(
				EmbeddedDataSourceConfiguration.class,
				HibernateJpaAutoConfiguration.class, TestConfiguration.class);
		this.context.getBean(TestRepository.class).doSomething();
	}

	@Test(expected = InvalidDataAccessApiUsageException.class)
	public void persistOfNullThrowsInvalidDataAccessApiUsageExceptionWithExceptionTranslation() {
		this.context = new AnnotationConfigApplicationContext(
				EmbeddedDataSourceConfiguration.class,
				HibernateJpaAutoConfiguration.class, TestConfiguration.class,
				PersistenceExceptionTranslationAutoConfiguration.class);
		this.context.getBean(TestRepository.class).doSomething();
	}

	@Configuration
	static class TestConfiguration {

		@Bean
		public TestRepository testRepository(EntityManagerFactory entityManagerFactory) {
			return new TestRepository(entityManagerFactory.createEntityManager());
		}
	}

	@Repository
	private static class TestRepository {

		private final EntityManager entityManager;

		TestRepository(EntityManager entityManager) {
			this.entityManager = entityManager;
		}

		public void doSomething() {
			this.entityManager.persist(null);
		}
	}

}
