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

package org.springframework.boot.orm.jpa;

import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceException;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link EntityScan}.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 */
@Deprecated
public class EntityScanTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private AnnotationConfigApplicationContext context;

	@After
	public void closeContext() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void simpleValue() throws Exception {
		this.context = new AnnotationConfigApplicationContext(ValueConfig.class);
		assertSetPackagesToScan("com.mycorp.entity");
	}

	@Test
	public void simpleValueAsm() throws Exception {
		this.context = new AnnotationConfigApplicationContext();
		this.context.registerBeanDefinition("valueConfig",
				new RootBeanDefinition(ValueConfig.class.getName()));
		this.context.refresh();
		assertSetPackagesToScan("com.mycorp.entity");
	}

	@Test
	public void needsEntityManageFactory() throws Exception {
		this.thrown.expect(IllegalStateException.class);
		this.thrown.expectMessage("Unable to configure "
				+ "LocalContainerEntityManagerFactoryBean from @EntityScan, "
				+ "ensure an appropriate bean is registered.");
		this.context = new AnnotationConfigApplicationContext(MissingEntityManager.class);
	}

	@Test
	public void userDeclaredBeanPostProcessorWithEntityManagerDependencyDoesNotPreventConfigurationOfPackagesToScan() {
		this.context = new AnnotationConfigApplicationContext(
				BeanPostProcessorConfiguration.class, BaseConfig.class);
		assertSetPackagesToScan("com.mycorp.entity");
	}

	private void assertSetPackagesToScan(String... expected) {
		String[] actual = this.context
				.getBean(TestLocalContainerEntityManagerFactoryBean.class)
				.getPackagesToScan();
		assertThat(actual).isEqualTo(expected);
	}

	@Configuration
	static class BaseConfig {

		@Bean
		public TestLocalContainerEntityManagerFactoryBean entityManagerFactoryBean() {
			return new TestLocalContainerEntityManagerFactoryBean();
		}

	}

	@EntityScan("com.mycorp.entity")
	@SuppressWarnings("deprecation")
	static class ValueConfig extends BaseConfig {
	}

	@Configuration
	@EntityScan("com.mycorp.entity")
	@SuppressWarnings("deprecation")
	static class MissingEntityManager {
	}

	@Configuration
	@EntityScan("com.mycorp.entity")
	@SuppressWarnings("deprecation")
	static class BeanPostProcessorConfiguration {

		protected final EntityManagerFactory entityManagerFactory;

		BeanPostProcessorConfiguration(EntityManagerFactory entityManagerFactory) {
			this.entityManagerFactory = entityManagerFactory;
		}

		@Bean
		public BeanPostProcessor beanPostProcessor() {
			return new BeanPostProcessor() {

				@Override
				public Object postProcessBeforeInitialization(Object bean,
						String beanName) throws BeansException {
					return bean;
				}

				@Override
				public Object postProcessAfterInitialization(Object bean, String beanName)
						throws BeansException {
					return bean;
				}
			};

		}
	}

	private static class TestLocalContainerEntityManagerFactoryBean
			extends LocalContainerEntityManagerFactoryBean {

		private String[] packagesToScan;

		@Override
		protected EntityManagerFactory createNativeEntityManagerFactory()
				throws PersistenceException {
			return mock(EntityManagerFactory.class);
		}

		@Override
		public void setPackagesToScan(String... packagesToScan) {
			this.packagesToScan = packagesToScan;
		}

		public String[] getPackagesToScan() {
			return this.packagesToScan;
		}

	}

}
