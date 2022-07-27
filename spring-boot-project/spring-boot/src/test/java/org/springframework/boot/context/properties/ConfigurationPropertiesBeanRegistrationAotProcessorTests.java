/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.context.properties;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.aot.AotServices;
import org.springframework.beans.factory.aot.BeanRegistrationAotContribution;
import org.springframework.beans.factory.aot.BeanRegistrationAotProcessor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.beans.factory.support.RootBeanDefinition;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ConfigurationPropertiesBeanRegistrationAotProcessor}.
 *
 * @author Stephane Nicoll
 */
class ConfigurationPropertiesBeanRegistrationAotProcessorTests {

	private final DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();

	private final ConfigurationPropertiesBeanRegistrationAotProcessor processor = new ConfigurationPropertiesBeanRegistrationAotProcessor();

	@Test
	void configurationPropertiesBeanRegistrationAotProcessorIsRegistered() {
		assertThat(AotServices.factories().load(BeanRegistrationAotProcessor.class))
				.anyMatch(ConfigurationPropertiesBeanRegistrationAotProcessor.class::isInstance);
	}

	@Test
	void processAheadOfTimeWithNoConfigurationPropertiesBean() {
		RootBeanDefinition beanDefinition = new RootBeanDefinition(String.class);
		this.beanFactory.registerBeanDefinition("test", beanDefinition);
		BeanRegistrationAotContribution contribution = this.processor
				.processAheadOfTime(RegisteredBean.of(this.beanFactory, "test"));
		assertThat(contribution).isNull();
	}

	@Test
	void processAheadOfTimeWithJavaBeanConfigurationPropertiesBean() {
		BeanRegistrationAotContribution contribution = process(JavaBeanSampleBean.class);
		assertThat(contribution).isNull();
	}

	@Test
	void processAheadOfTimeWithValueObjectConfigurationPropertiesBean() {
		BeanRegistrationAotContribution contribution = process(ValueObjectSampleBean.class);
		assertThat(contribution).isNotNull();
	}

	private BeanRegistrationAotContribution process(Class<?> type) {
		ConfigurationPropertiesBeanRegistrar beanRegistrar = new ConfigurationPropertiesBeanRegistrar(this.beanFactory);
		beanRegistrar.register(type);
		RegisteredBean registeredBean = RegisteredBean.of(this.beanFactory,
				this.beanFactory.getBeanDefinitionNames()[0]);
		return this.processor.processAheadOfTime(registeredBean);
	}

	@ConfigurationProperties("test")
	public static class JavaBeanSampleBean {

		private String name;

		public String getName() {
			return this.name;
		}

		public void setName(String name) {
			this.name = name;
		}

	}

	@ConfigurationProperties("test")
	public static class ValueObjectSampleBean {

		@SuppressWarnings("unused")
		private final String name;

		ValueObjectSampleBean(String name) {
			this.name = name;
		}

	}

}
