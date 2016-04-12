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

package org.springframework.boot.autoconfigure.transaction;

import java.util.Map;

import org.junit.After;
import org.junit.Test;

import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.test.util.EnvironmentTestUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link TransactionAutoConfiguration}.
 *
 * @author Stephane Nicoll
 */
public class TransactionAutoConfigurationTests {

	private AnnotationConfigApplicationContext context;

	@After
	public void tearDown() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void noTransactionManager() {
		load(EmptyConfiguration.class);
		assertThat(this.context.getBeansOfType(TransactionTemplate.class)).isEmpty();
	}

	@Test
	public void singleTransactionManager() {
		load(DataSourceAutoConfiguration.class,
				DataSourceTransactionManagerAutoConfiguration.class);
		PlatformTransactionManager transactionManager = this.context
				.getBean(PlatformTransactionManager.class);
		TransactionTemplate transactionTemplate = this.context
				.getBean(TransactionTemplate.class);
		assertThat(transactionTemplate.getTransactionManager())
				.isSameAs(transactionManager);
	}

	@Test
	public void severalTransactionManagers() {
		load(SeveralTransactionManagersConfiguration.class);
		assertThat(this.context.getBeansOfType(TransactionTemplate.class)).isEmpty();
	}

	@Test
	public void customTransactionManager() {
		load(CustomTransactionManagerConfiguration.class);
		Map<String, TransactionTemplate> beans = this.context
				.getBeansOfType(TransactionTemplate.class);
		assertThat(beans).hasSize(1);
		assertThat(beans.containsKey("transactionTemplateFoo")).isTrue();
	}

	private void load(Class<?>... configs) {
		AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();
		applicationContext.register(configs);
		applicationContext.register(TransactionAutoConfiguration.class);
		EnvironmentTestUtils.addEnvironment(applicationContext,
				"spring.datasource.initialize:false");
		applicationContext.refresh();
		this.context = applicationContext;
	}

	@Configuration
	static class EmptyConfiguration {
	}

	@Configuration
	static class SeveralTransactionManagersConfiguration {

		@Bean
		public PlatformTransactionManager transactionManagerOne() {
			return mock(PlatformTransactionManager.class);
		}

		@Bean
		public PlatformTransactionManager transactionManagerTwo() {
			return mock(PlatformTransactionManager.class);
		}
	}

	@Configuration
	static class CustomTransactionManagerConfiguration {

		@Bean
		public TransactionTemplate transactionTemplateFoo() {
			return new TransactionTemplate(transactionManagerFoo());
		}

		@Bean
		public PlatformTransactionManager transactionManagerFoo() {
			return mock(PlatformTransactionManager.class);
		}

	}

}
