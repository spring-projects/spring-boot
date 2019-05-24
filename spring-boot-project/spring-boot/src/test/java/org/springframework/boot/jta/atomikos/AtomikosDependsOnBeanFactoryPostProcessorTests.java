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

package org.springframework.boot.jta.atomikos;

import java.util.Arrays;
import java.util.HashSet;

import javax.jms.ConnectionFactory;
import javax.sql.DataSource;

import com.atomikos.icatch.jta.UserTransactionManager;
import com.atomikos.jms.extra.MessageDrivenContainer;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link AtomikosDependsOnBeanFactoryPostProcessor}.
 *
 * @author Phillip Webb
 */
class AtomikosDependsOnBeanFactoryPostProcessorTests {

	private AnnotationConfigApplicationContext context;

	@Test
	void setsDependsOn() {
		this.context = new AnnotationConfigApplicationContext(Config.class);
		assertDependsOn("dataSource");
		assertDependsOn("connectionFactory");
		assertDependsOn("userTransactionManager", "dataSource", "connectionFactory");
		assertDependsOn("messageDrivenContainer", "userTransactionManager");
		this.context.close();
	}

	private void assertDependsOn(String bean, String... expected) {
		BeanDefinition definition = this.context.getBeanDefinition(bean);
		if (definition.getDependsOn() == null) {
			assertThat(expected).as("No dependsOn expected for " + bean).isEmpty();
			return;
		}
		HashSet<String> dependsOn = new HashSet<>(Arrays.asList(definition.getDependsOn()));
		assertThat(dependsOn).isEqualTo(new HashSet<>(Arrays.asList(expected)));
	}

	@Configuration(proxyBeanMethods = false)
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
		public UserTransactionManager userTransactionManager() {
			return mock(UserTransactionManager.class);
		}

		@Bean
		public MessageDrivenContainer messageDrivenContainer() {
			return mock(MessageDrivenContainer.class);
		}

		@Bean
		public static AtomikosDependsOnBeanFactoryPostProcessor atomikosPostProcessor() {
			return new AtomikosDependsOnBeanFactoryPostProcessor();
		}

	}

}
