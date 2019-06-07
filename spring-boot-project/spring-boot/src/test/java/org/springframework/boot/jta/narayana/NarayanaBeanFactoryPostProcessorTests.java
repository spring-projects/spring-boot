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

package org.springframework.boot.jta.narayana;

import javax.jms.ConnectionFactory;
import javax.sql.DataSource;
import javax.transaction.TransactionManager;

import org.junit.Test;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link NarayanaBeanFactoryPostProcessor}.
 *
 * @author Gytis Trikleris
 */
public class NarayanaBeanFactoryPostProcessorTests {

	private AnnotationConfigApplicationContext context;

	@Test
	public void setsDependsOn() {
		DefaultListableBeanFactory beanFactory = spy(new DefaultListableBeanFactory());
		this.context = new AnnotationConfigApplicationContext(beanFactory);
		this.context.register(Config.class);
		this.context.refresh();
		verify(beanFactory).registerDependentBean("narayanaTransactionManager", "dataSource");
		verify(beanFactory).registerDependentBean("narayanaTransactionManager", "connectionFactory");
		verify(beanFactory).registerDependentBean("narayanaRecoveryManagerBean", "dataSource");
		verify(beanFactory).registerDependentBean("narayanaRecoveryManagerBean", "connectionFactory");
		this.context.close();
	}

	@Configuration
	static class Config {

		@Bean
		public DataSource dataSource() {
			return mock(DataSource.class);
		}

		@Bean
		public ConnectionFactory connectionFactory() {
			return mock(ConnectionFactory.class);
		}

		@Bean
		public TransactionManager narayanaTransactionManager() {
			return mock(TransactionManager.class);
		}

		@Bean
		public NarayanaRecoveryManagerBean narayanaRecoveryManagerBean() {
			return mock(NarayanaRecoveryManagerBean.class);
		}

		@Bean
		public static NarayanaBeanFactoryPostProcessor narayanaPostProcessor() {
			return new NarayanaBeanFactoryPostProcessor();
		}

	}

}
