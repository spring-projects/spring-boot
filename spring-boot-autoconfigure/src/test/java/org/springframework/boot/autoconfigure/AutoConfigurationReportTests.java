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

package org.springframework.boot.autoconfigure;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.autoconfigure.jms.JmsTemplateAutoConfiguration;
import org.springframework.boot.autoconfigure.report.AutoConfigurationReport;
import org.springframework.boot.autoconfigure.report.CreatedBeanInfo;
import org.springframework.boot.autoconfigure.report.EnableAutoConfigurationReport;
import org.springframework.boot.autoconfigure.web.MultipartAutoConfiguration;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.jms.core.JmsTemplate;

/**
 * Tests for {@link AutoConfigurationReport}.
 *
 * @author Greg Turnquist
 */
public class AutoConfigurationReportTests {

	private AnnotationConfigApplicationContext context;

	@Test
	public void simpleReportTestCase() {
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(TestConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		AutoConfigurationReport autoconfigSettings = this.context
				.getBean(AutoConfigurationReport.class);

		Set<CreatedBeanInfo> beansBootCreated = autoconfigSettings.getBeansCreated();
		Set<String> beanNamesBootCreated = autoconfigSettings.getBeanNamesCreated();
		Set<Class<?>> beanTypesBootCreated = autoconfigSettings.getBeanTypesCreated();

		assertEquals(1, beansBootCreated.size());
		assertEquals(1, beanNamesBootCreated.size());
		assertEquals(1, beanTypesBootCreated.size());

		assertTrue(beanNamesBootCreated.contains("propertySourcesPlaceholderConfigurer"));
		assertTrue(beanTypesBootCreated
				.contains(PropertySourcesPlaceholderConfigurer.class));

		boolean foundPropertySourcesPlaceHolderConfigurer = false;
		int totalDecisions = 0;
		for (CreatedBeanInfo item : beansBootCreated) {
			for (String decision : item.getDecisions()) {
				totalDecisions += 1;
				if (decision.contains("propertySourcesPlaceholderConfigurer matched")) {
					foundPropertySourcesPlaceHolderConfigurer = true;
				}
			}
		}
		assertEquals(1, totalDecisions);
		assertTrue(foundPropertySourcesPlaceHolderConfigurer);
	}

	@Test
	public void rabbitReportTest() {
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(TestConfiguration.class, RabbitAutoConfiguration.class);
		this.context.refresh();
		AutoConfigurationReport autoconfigSettings = this.context
				.getBean(AutoConfigurationReport.class);

		Set<CreatedBeanInfo> beansBootCreated = autoconfigSettings.getBeansCreated();
		Set<String> beanNamesBootCreated = autoconfigSettings.getBeanNamesCreated();
		Set<Class<?>> beanTypesBootCreated = autoconfigSettings.getBeanTypesCreated();

		assertEquals(3, beansBootCreated.size());
		assertEquals(3, beanNamesBootCreated.size());
		assertEquals(3, beanTypesBootCreated.size());

		assertTrue(beanNamesBootCreated.contains("amqpAdmin"));
		assertTrue(beanNamesBootCreated.contains("rabbitConnectionFactory"));
		assertTrue(beanNamesBootCreated.contains("rabbitTemplate"));

		assertTrue(beanTypesBootCreated.contains(RabbitAdmin.class));
		assertTrue(beanTypesBootCreated.contains(ConnectionFactory.class));
		assertTrue(beanTypesBootCreated.contains(RabbitTemplate.class));

		boolean foundRabbitConnectionFactory = false;
		boolean foundAmqpAdminExpressionCondition = false;
		boolean foundAmqpAdminBeanCondition = false;
		boolean foundRabbitTemplateCondition = false;
		int totalDecisions = 0;
		for (CreatedBeanInfo item : beansBootCreated) {
			for (String decision : item.getDecisions()) {
				totalDecisions += 1;
				if (decision.contains("RabbitConnectionFactoryCreator matched")) {
					foundRabbitConnectionFactory = true;
				} else if (decision.contains("OnExpressionCondition")
						&& decision.contains("amqpAdmin matched due to SpEL expression")) {
					foundAmqpAdminExpressionCondition = true;
				} else if (decision.contains("OnBeanCondition")
						&& decision.contains("amqpAdmin matched")) {
					foundAmqpAdminBeanCondition = true;
				} else if (decision.contains("rabbitTemplate matched")) {
					foundRabbitTemplateCondition = true;
				}
			}
		}
		assertEquals(4, totalDecisions);
		assertTrue(foundRabbitConnectionFactory);
		assertTrue(foundAmqpAdminExpressionCondition);
		assertTrue(foundAmqpAdminBeanCondition);
		assertTrue(foundRabbitTemplateCondition);
	}

	@Test
	public void verifyItGathersNegativeMatches() {
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(TestConfiguration2.class,
				JmsTemplateAutoConfiguration.class, MultipartAutoConfiguration.class);
		this.context.refresh();
		AutoConfigurationReport autoconfigSettings = this.context
				.getBean(AutoConfigurationReport.class);

		Map<String, List<String>> negatives = autoconfigSettings.getNegativeDecisions();

		boolean foundMyOwnJmsTemplateAndBackedOff = false;
		boolean didNotFindMultipartConfigElement = false;
		int totalNegativeDecisions = 0;
		for (String key : negatives.keySet()) {
			for (String decision : negatives.get(key)) {
				totalNegativeDecisions += 1;
				if (decision
						.contains("JmsTemplateAutoConfiguration#jmsTemplate did not match")
						&& decision.contains("found the following [myOwnJmsTemplate]")) {
					foundMyOwnJmsTemplateAndBackedOff = true;
				} else if (decision.contains("MultipartAutoConfiguration did not match")
						&& decision
								.contains("list['javax.servlet.MultipartConfigElement']")
						&& decision.contains("found no beans")) {
					didNotFindMultipartConfigElement = true;
				}
			}
		}
		// varying situations might cause multi-conditional beans to evaluate in different orders
		assertTrue(totalNegativeDecisions >= 2);
		assertTrue(foundMyOwnJmsTemplateAndBackedOff);
		assertTrue(didNotFindMultipartConfigElement);

	}

	@Configuration
	@EnableAutoConfigurationReport
	public static class TestConfiguration {
	}

	@Configuration
	@EnableAutoConfigurationReport
	public static class TestConfiguration2 {
		@Bean
		JmsTemplate myOwnJmsTemplate(javax.jms.ConnectionFactory connectionFactory) {
			return new JmsTemplate(connectionFactory);
		}
	}

}
