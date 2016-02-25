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

package org.springframework.boot.autoconfigure.jms.activemq;

import javax.jms.ConnectionFactory;
import javax.jms.JMSException;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.pool.PooledConnectionFactory;
import org.junit.Test;

import org.springframework.boot.autoconfigure.jms.JmsAutoConfiguration;
import org.springframework.boot.test.EnvironmentTestUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;

/**
 * Tests for {@link ActiveMQAutoConfiguration}
 *
 * @author Andy Wilkinson
 */
public class ActiveMQAutoConfigurationTests {

	private AnnotationConfigApplicationContext context;

	@Test
	public void brokerIsEmbeddedByDefault() {
		load(EmptyConfiguration.class);
		ConnectionFactory connectionFactory = this.context
				.getBean(ConnectionFactory.class);
		assertThat(connectionFactory).isInstanceOf(ActiveMQConnectionFactory.class);
		String brokerUrl = ((ActiveMQConnectionFactory) connectionFactory).getBrokerURL();
		assertThat(brokerUrl).isEqualTo("vm://localhost?broker.persistent=false");
	}

	@Test
	public void configurationBacksOffWhenCustomConnectionFactoryExists() {
		load(CustomConnectionFactoryConfiguration.class);
		assertThat(mockingDetails(this.context.getBean(ConnectionFactory.class)).isMock())
				.isTrue();
	}

	@Test
	public void pooledConnectionFactoryConfiguration() throws JMSException {
		load(EmptyConfiguration.class, "spring.activemq.pooled:true");
		ConnectionFactory connectionFactory = this.context
				.getBean(ConnectionFactory.class);
		assertThat(connectionFactory).isInstanceOf(PooledConnectionFactory.class);
		this.context.close();
		assertThat(((PooledConnectionFactory) connectionFactory).createConnection())
				.isNull();
	}

	private void load(Class<?> config, String... environment) {
		this.context = doLoad(config, environment);
	}

	private AnnotationConfigApplicationContext doLoad(Class<?> config,
			String... environment) {
		AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();
		applicationContext.register(config);
		applicationContext.register(ActiveMQAutoConfiguration.class,
				JmsAutoConfiguration.class);
		EnvironmentTestUtils.addEnvironment(applicationContext, environment);
		applicationContext.refresh();
		return applicationContext;
	}

	@Configuration
	static class EmptyConfiguration {

	}

	@Configuration
	static class CustomConnectionFactoryConfiguration {

		@Bean
		public ConnectionFactory connectionFactory() {
			return mock(ConnectionFactory.class);
		}
	}

}
