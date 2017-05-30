/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.autoconfigure.jms.amqp10;

import javax.jms.ConnectionFactory;

import org.apache.qpid.jms.JmsConnectionFactory;
import org.junit.After;
import org.junit.Test;

import org.springframework.boot.autoconfigure.jms.JmsAutoConfiguration;
import org.springframework.boot.test.util.EnvironmentTestUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.core.JmsTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test that Qpid JMS Auto Configuration works.
 *
 * @author Timothy Bish
 */
public class QpidJMSAutoConfigurationTest {

	private AnnotationConfigApplicationContext context;

	@After
	public void tearDown() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void testDefaultsToLocalURI() {
		load(EmptyConfiguration.class);

		JmsTemplate jmsTemplate = this.context.getBean(JmsTemplate.class);
		ConnectionFactory connectionFactory = this.context
				.getBean(ConnectionFactory.class);

		assertThat(connectionFactory instanceof JmsConnectionFactory).isTrue();

		JmsConnectionFactory qpidJmsFactory = (JmsConnectionFactory) connectionFactory;

		assertThat(jmsTemplate.getConnectionFactory()).isEqualTo(connectionFactory);
		assertThat(qpidJmsFactory.getRemoteURI()).isEqualTo("amqp://localhost:5672");
		assertThat(qpidJmsFactory.getUsername()).isNotNull();
		assertThat(qpidJmsFactory.getPassword()).isNotNull();
	}

	@Test
	public void testCustomConnectionFactorySettings() {
		load(EmptyConfiguration.class, "spring.qpidjms.remoteURL=amqp://127.0.0.1:5672",
				"spring.qpidjms.username=foo", "spring.qpidjms.password=bar");

		JmsTemplate jmsTemplate = this.context.getBean(JmsTemplate.class);
		JmsConnectionFactory connectionFactory = this.context
				.getBean(JmsConnectionFactory.class);

		assertThat(jmsTemplate.getConnectionFactory()).isEqualTo(connectionFactory);

		assertThat(connectionFactory.getRemoteURI()).isEqualTo("amqp://127.0.0.1:5672");
		assertThat(connectionFactory.getUsername()).isEqualTo("foo");
		assertThat(connectionFactory.getPassword()).isEqualTo("bar");
	}

	@Test
	public void testReceiveLocalOnlyOptionsAppliedFromEnv() {
		load(EmptyConfiguration.class, "spring.qpidjms.receiveLocalOnly=true",
				"spring.qpidjms.receiveNoWaitLocalOnly=true");

		JmsTemplate jmsTemplate = this.context.getBean(JmsTemplate.class);
		JmsConnectionFactory connectionFactory = this.context
				.getBean(JmsConnectionFactory.class);

		assertThat(jmsTemplate.getConnectionFactory()).isEqualTo(connectionFactory);

		assertThat(connectionFactory.isReceiveLocalOnly()).isTrue();
		assertThat(connectionFactory.isReceiveNoWaitLocalOnly()).isTrue();
	}

	@Test
	public void testReceiveLocalOnlyOptionsAppliedFromEnvOverridesURI() {
		load(EmptyConfiguration.class,
				"spring.qpidjms.remoteURL=amqp://127.0.0.1:5672"
						+ "?jms.receiveLocalOnly=false&jms.receiveNoWaitLocalOnly=false",
				"spring.qpidjms.receiveLocalOnly=true",
				"spring.qpidjms.receiveNoWaitLocalOnly=true");

		JmsTemplate jmsTemplate = this.context.getBean(JmsTemplate.class);
		JmsConnectionFactory connectionFactory = this.context
				.getBean(JmsConnectionFactory.class);

		assertThat(jmsTemplate.getConnectionFactory()).isEqualTo(connectionFactory);

		assertThat(connectionFactory.isReceiveLocalOnly()).isTrue();
		assertThat(connectionFactory.isReceiveNoWaitLocalOnly()).isTrue();
	}

	private void load(Class<?> config, String... environment) {
		AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();
		applicationContext.register(config);
		applicationContext.register(QpidJMSAutoConfiguration.class,
				JmsAutoConfiguration.class);
		EnvironmentTestUtils.addEnvironment(applicationContext, environment);
		applicationContext.refresh();
		this.context = applicationContext;
	}

	@Configuration
	static class EmptyConfiguration {
	}
}
