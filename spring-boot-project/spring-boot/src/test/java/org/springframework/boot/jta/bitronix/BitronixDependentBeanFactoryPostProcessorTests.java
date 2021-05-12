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

package org.springframework.boot.jta.bitronix;

import javax.jms.ConnectionFactory;
import javax.sql.DataSource;

import bitronix.tm.BitronixTransactionManager;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link BitronixDependentBeanFactoryPostProcessor}.
 *
 * @author Phillip Webb
 */
@Deprecated
class BitronixDependentBeanFactoryPostProcessorTests {

	private AnnotationConfigApplicationContext context;

	@Test
	void setsDependsOn() {
		DefaultListableBeanFactory beanFactory = spy(new DefaultListableBeanFactory());
		this.context = new AnnotationConfigApplicationContext(beanFactory);
		this.context.register(Config.class);
		this.context.refresh();
		String name = "bitronixTransactionManager";
		verify(beanFactory).registerDependentBean(name, "dataSource");
		verify(beanFactory).registerDependentBean(name, "connectionFactory");
		this.context.close();
	}

	@Configuration(proxyBeanMethods = false)
	static class Config {

		@Bean
		DataSource dataSource() {
			return mock(DataSource.class);
		}

		@Bean
		ConnectionFactory connectionFactory() {
			return mock(ConnectionFactory.class);
		}

		@Bean
		BitronixTransactionManager bitronixTransactionManager() {
			return mock(BitronixTransactionManager.class);
		}

		@Bean
		static BitronixDependentBeanFactoryPostProcessor bitronixPostProcessor() {
			return new BitronixDependentBeanFactoryPostProcessor();
		}

	}

}
