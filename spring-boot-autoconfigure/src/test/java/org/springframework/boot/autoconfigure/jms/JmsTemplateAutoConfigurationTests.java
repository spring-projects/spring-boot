/*
 * Copyright 2012-2013 the original author or authors.
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

package org.springframework.boot.autoconfigure.jms;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import javax.jms.ConnectionFactory;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.junit.Test;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.core.JmsTemplate;

/**
 * Tests for {@link JmsTemplateAutoConfiguration}.
 * 
 * @author Greg Turnquist
 */
public class JmsTemplateAutoConfigurationTests {

	private AnnotationConfigApplicationContext context;

	@Test
	public void testDefaultJmsTemplate() {
		this.context = new AnnotationConfigApplicationContext();
		this.context
				.register(TestConfiguration.class, JmsTemplateAutoConfiguration.class);
		this.context.refresh();
		JmsTemplate jmsTemplate = this.context.getBean(JmsTemplate.class);
		ActiveMQConnectionFactory connectionFactory = this.context
				.getBean(ActiveMQConnectionFactory.class);
		assertNotNull(jmsTemplate);
		assertNotNull(connectionFactory);
		assertEquals(jmsTemplate.getConnectionFactory(), connectionFactory);
	}

	@Configuration
	protected static class TestConfiguration {
	}

	@Test
	public void testConnectionFactoryBackoff() {
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(TestConfiguration2.class,
				JmsTemplateAutoConfiguration.class);
		this.context.refresh();
		assertEquals("foobar", this.context.getBean(ActiveMQConnectionFactory.class)
				.getBrokerURL());
	}

	@Configuration
	protected static class TestConfiguration2 {
		@Bean
		ConnectionFactory connectionFactory() {
			return new ActiveMQConnectionFactory() {
				{
					setBrokerURL("foobar");
				}
			};
		}
	}

	@Test
	public void testJmsTemplateBackoff() {
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(TestConfiguration3.class,
				JmsTemplateAutoConfiguration.class);
		this.context.refresh();
		JmsTemplate jmsTemplate = this.context.getBean(JmsTemplate.class);
		assertEquals(999, jmsTemplate.getPriority());
	}

	@Configuration
	protected static class TestConfiguration3 {
		@Bean
		JmsTemplate jmsTemplate(ConnectionFactory connectionFactory) {
			JmsTemplate jmsTemplate = new JmsTemplate(connectionFactory);
			jmsTemplate.setPriority(999);
			return jmsTemplate;
		}

	}

	@Test
	public void testJmsTemplateBackoffEverything() {
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(TestConfiguration2.class, TestConfiguration3.class,
				JmsTemplateAutoConfiguration.class);
		this.context.refresh();
		JmsTemplate jmsTemplate = this.context.getBean(JmsTemplate.class);
		assertEquals(999, jmsTemplate.getPriority());
		assertEquals("foobar", this.context.getBean(ActiveMQConnectionFactory.class)
				.getBrokerURL());
	}

	@Test
	public void testPubSubEnabledByDefault() {
		this.context = new AnnotationConfigApplicationContext();
		this.context
				.register(TestConfiguration.class, JmsTemplateAutoConfiguration.class);
		this.context.refresh();
		JmsTemplate jmsTemplate = this.context.getBean(JmsTemplate.class);
		assertTrue(jmsTemplate.isPubSubDomain());
	}

	@Test
	public void testJmsTemplatePostProcessedSoThatPubSubIsFalse() {
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(TestConfiguration4.class,
				JmsTemplateAutoConfiguration.class);
		this.context.refresh();
		JmsTemplate jmsTemplate = this.context.getBean(JmsTemplate.class);
		assertFalse(jmsTemplate.isPubSubDomain());
	}

	@Configuration
	protected static class TestConfiguration4 implements BeanPostProcessor {
		@Override
		public Object postProcessAfterInitialization(Object bean, String beanName)
				throws BeansException {
			if (bean.getClass().isAssignableFrom(JmsTemplate.class)) {
				JmsTemplate jmsTemplate = (JmsTemplate) bean;
				jmsTemplate.setPubSubDomain(false);
			}
			return bean;
		}

		@Override
		public Object postProcessBeforeInitialization(Object bean, String beanName)
				throws BeansException {
			return bean;
		}
	}

}
