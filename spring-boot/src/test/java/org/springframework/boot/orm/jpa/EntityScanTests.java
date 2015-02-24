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

package org.springframework.boot.orm.jpa;

import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link EntityScan}.
 *
 * @author Phillip Webb
 */
public class EntityScanTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private AnnotationConfigApplicationContext context;

	@Test
	public void testValue() throws Exception {
		this.context = new AnnotationConfigApplicationContext(ValueConfig.class);
		assertSetPackagesToScan("com.mycorp.entity");
	}

	@Test
	public void basePackages() throws Exception {
		this.context = new AnnotationConfigApplicationContext(BasePackagesConfig.class);
		assertSetPackagesToScan("com.mycorp.entity2");
	}

	@Test
	public void basePackageClasses() throws Exception {
		this.context = new AnnotationConfigApplicationContext(
				BasePackageClassesConfig.class);
		assertSetPackagesToScan(getClass().getPackage().getName());
	}

	@Test
	public void fromConfigurationClass() throws Exception {
		this.context = new AnnotationConfigApplicationContext(FromConfigConfig.class);
		assertSetPackagesToScan(getClass().getPackage().getName());
	}

	@Test
	public void valueAndBasePackagesThrows() throws Exception {
		this.thrown.expect(IllegalStateException.class);
		this.thrown.expectMessage("@EntityScan basePackages and value "
				+ "attributes are mutually exclusive");
		this.context = new AnnotationConfigApplicationContext(ValueAndBasePackages.class);
	}

	@Test
	public void valueAndBasePackageClassesMerges() throws Exception {
		this.context = new AnnotationConfigApplicationContext(
				ValueAndBasePackageClasses.class);
		assertSetPackagesToScan("com.mycorp.entity", getClass().getPackage().getName());
	}

	@Test
	public void basePackageAndBasePackageClassesMerges() throws Exception {
		this.context = new AnnotationConfigApplicationContext(
				BasePackagesAndBasePackageClasses.class);
		assertSetPackagesToScan("com.mycorp.entity2", getClass().getPackage().getName());
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
		String[] actual = this.context.getBean(
				TestLocalContainerEntityManagerFactoryBean.class).getPackagesToScan();
		assertThat(actual, equalTo(expected));
	}

	@Configuration
	static class BaseConfig {

		@Bean
		public TestLocalContainerEntityManagerFactoryBean entityManagerFactoryBean() {
			return new TestLocalContainerEntityManagerFactoryBean();
		}

	}

	@EntityScan("com.mycorp.entity")
	static class ValueConfig extends BaseConfig {
	}

	@EntityScan(basePackages = "com.mycorp.entity2")
	static class BasePackagesConfig extends BaseConfig {
	}

	@EntityScan(basePackageClasses = EntityScanTests.class)
	static class BasePackageClassesConfig extends BaseConfig {
	}

	@EntityScan
	static class FromConfigConfig extends BaseConfig {
	}

	@EntityScan(value = "com.mycorp.entity", basePackages = "com.mycorp")
	static class ValueAndBasePackages extends BaseConfig {
	}

	@EntityScan(value = "com.mycorp.entity", basePackageClasses = EntityScanTests.class)
	static class ValueAndBasePackageClasses extends BaseConfig {
	}

	@EntityScan(basePackages = "com.mycorp.entity2", basePackageClasses = EntityScanTests.class)
	static class BasePackagesAndBasePackageClasses extends BaseConfig {
	}

	@Configuration
	@EntityScan("com.mycorp.entity")
	static class MissingEntityManager {
	}

	@Configuration
	@EntityScan("com.mycorp.entity")
	static class BeanPostProcessorConfiguration {

		@Autowired
		private EntityManagerFactory entityManagerFactory;

		@Bean
		public BeanPostProcessor beanPostProcessor() {
			return new BeanPostProcessor() {

				@Override
				public Object postProcessBeforeInitialization(Object bean, String beanName)
						throws BeansException {
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

	private static class TestLocalContainerEntityManagerFactoryBean extends
			LocalContainerEntityManagerFactoryBean {

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
