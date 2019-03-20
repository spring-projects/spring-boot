/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.autoconfigure.orm.jpa;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import org.hibernate.engine.transaction.jta.platform.internal.NoJtaPlatform;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.autoconfigure.TestAutoConfigurationPackage;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.EmbeddedDataSourceConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.test.City;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.boot.test.util.EnvironmentTestUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.persistenceunit.DefaultPersistenceUnitManager;
import org.springframework.orm.jpa.persistenceunit.PersistenceUnitManager;
import org.springframework.orm.jpa.support.OpenEntityManagerInViewFilter;
import org.springframework.orm.jpa.support.OpenEntityManagerInViewInterceptor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Base for JPA tests and tests for {@link JpaBaseConfiguration}.
 *
 * @author Phillip Webb
 * @author Dave Syer
 */
public abstract class AbstractJpaAutoConfigurationTests {

	@Rule
	public ExpectedException expected = ExpectedException.none();

	protected AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

	@After
	public void close() {
		this.context.close();
	}

	protected abstract Class<?> getAutoConfigureClass();

	@Test
	public void testNoDataSource() throws Exception {
		this.context.register(PropertyPlaceholderAutoConfiguration.class,
				getAutoConfigureClass());
		this.expected.expect(BeanCreationException.class);
		this.expected.expectMessage("No qualifying bean");
		this.expected.expectMessage("DataSource");
		this.context.refresh();
	}

	@Test
	public void testEntityManagerCreated() throws Exception {
		setupTestConfiguration();
		this.context.refresh();
		assertThat(this.context.getBean(DataSource.class)).isNotNull();
		assertThat(this.context.getBean(JpaTransactionManager.class)).isNotNull();
	}

	@Test
	public void testDataSourceTransactionManagerNotCreated() throws Exception {
		this.context.register(DataSourceTransactionManagerAutoConfiguration.class);
		setupTestConfiguration();
		this.context.refresh();
		assertThat(this.context.getBean(DataSource.class)).isNotNull();
		assertThat(this.context.getBean("transactionManager"))
				.isInstanceOf(JpaTransactionManager.class);
	}

	@Test
	public void testOpenEntityManagerInViewInterceptorCreated() throws Exception {
		AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
		context.register(TestConfiguration.class, EmbeddedDataSourceConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class, getAutoConfigureClass());
		context.refresh();
		assertThat(context.getBean(OpenEntityManagerInViewInterceptor.class)).isNotNull();
		context.close();
	}

	@Test
	public void testOpenEntityManagerInViewInterceptorNotRegisteredWhenFilterPresent()
			throws Exception {
		AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
		context.register(TestFilterConfiguration.class,
				EmbeddedDataSourceConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class, getAutoConfigureClass());
		context.refresh();
		assertThat(getInterceptorBeans(context).length).isEqualTo(0);
		context.close();
	}

	@Test
	public void testOpenEntityManagerInViewInterceptorNotRegisteredWhenExplicitlyOff()
			throws Exception {
		AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
		EnvironmentTestUtils.addEnvironment(context, "spring.jpa.open_in_view:false");
		context.register(TestConfiguration.class, EmbeddedDataSourceConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class, getAutoConfigureClass());
		context.refresh();
		assertThat(getInterceptorBeans(context).length).isEqualTo(0);
		context.close();
	}

	@Test
	public void customJpaProperties() throws Exception {
		EnvironmentTestUtils.addEnvironment(this.context, "spring.jpa.properties.a:b",
				"spring.jpa.properties.a.b:c", "spring.jpa.properties.c:d");
		setupTestConfiguration();
		this.context.refresh();
		LocalContainerEntityManagerFactoryBean bean = this.context
				.getBean(LocalContainerEntityManagerFactoryBean.class);
		Map<String, Object> map = bean.getJpaPropertyMap();
		assertThat(map.get("a")).isEqualTo("b");
		assertThat(map.get("c")).isEqualTo("d");
		assertThat(map.get("a.b")).isEqualTo("c");
	}

	@Test
	public void usesManuallyDefinedLocalContainerEntityManagerFactoryBeanIfAvailable() {
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.datasource.initialize:false");
		setupTestConfiguration(
				TestConfigurationWithLocalContainerEntityManagerFactoryBean.class);
		this.context.refresh();
		LocalContainerEntityManagerFactoryBean factoryBean = this.context
				.getBean(LocalContainerEntityManagerFactoryBean.class);
		Map<String, Object> map = factoryBean.getJpaPropertyMap();
		assertThat(map.get("configured")).isEqualTo("manually");
	}

	@Test
	public void usesManuallyDefinedEntityManagerFactoryIfAvailable() {
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.datasource.initialize:false");
		setupTestConfiguration(TestConfigurationWithEntityManagerFactory.class);
		this.context.refresh();
		EntityManagerFactory factoryBean = this.context
				.getBean(EntityManagerFactory.class);
		Map<String, Object> map = factoryBean.getProperties();
		assertThat(map.get("configured")).isEqualTo("manually");
	}

	@Test
	public void usesManuallyDefinedTransactionManagerBeanIfAvailable() {
		setupTestConfiguration(TestConfigurationWithTransactionManager.class);
		this.context.refresh();
		PlatformTransactionManager txManager = this.context
				.getBean(PlatformTransactionManager.class);
		assertThat(txManager).isInstanceOf(CustomJpaTransactionManager.class);
	}

	@Test
	public void customPersistenceUnitManager() throws Exception {
		setupTestConfiguration(TestConfigurationWithCustomPersistenceUnitManager.class);
		this.context.refresh();
		LocalContainerEntityManagerFactoryBean entityManagerFactoryBean = this.context
				.getBean(LocalContainerEntityManagerFactoryBean.class);
		Field field = LocalContainerEntityManagerFactoryBean.class
				.getDeclaredField("persistenceUnitManager");
		field.setAccessible(true);
		assertThat(field.get(entityManagerFactoryBean))
				.isEqualTo(this.context.getBean(PersistenceUnitManager.class));
	}

	protected void setupTestConfiguration() {
		setupTestConfiguration(TestConfiguration.class);
	}

	protected void setupTestConfiguration(Class<?> configClass) {
		this.context.register(configClass, EmbeddedDataSourceConfiguration.class,
				DataSourceAutoConfiguration.class, TransactionAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class, getAutoConfigureClass());
	}

	private String[] getInterceptorBeans(ApplicationContext context) {
		return context.getBeanNamesForType(OpenEntityManagerInViewInterceptor.class);
	}

	@Configuration
	@TestAutoConfigurationPackage(City.class)
	protected static class TestConfiguration {

	}

	@Configuration
	@TestAutoConfigurationPackage(City.class)
	protected static class TestFilterConfiguration {

		@Bean
		public OpenEntityManagerInViewFilter openEntityManagerInViewFilter() {
			return new OpenEntityManagerInViewFilter();
		}

	}

	@Configuration
	protected static class TestConfigurationWithLocalContainerEntityManagerFactoryBean
			extends TestConfiguration {

		@Bean
		public LocalContainerEntityManagerFactoryBean entityManagerFactory(
				DataSource dataSource, JpaVendorAdapter adapter) {
			LocalContainerEntityManagerFactoryBean factoryBean = new LocalContainerEntityManagerFactoryBean();
			factoryBean.setJpaVendorAdapter(adapter);
			factoryBean.setDataSource(dataSource);
			factoryBean.setPersistenceUnitName("manually-configured");
			Map<String, Object> properties = new HashMap<String, Object>();
			properties.put("configured", "manually");
			properties.put("hibernate.transaction.jta.platform", NoJtaPlatform.INSTANCE);
			factoryBean.setJpaPropertyMap(properties);
			return factoryBean;
		}

	}

	@Configuration
	protected static class TestConfigurationWithEntityManagerFactory
			extends TestConfiguration {

		@Bean
		public EntityManagerFactory entityManagerFactory(DataSource dataSource,
				JpaVendorAdapter adapter) {
			LocalContainerEntityManagerFactoryBean factoryBean = new LocalContainerEntityManagerFactoryBean();
			factoryBean.setJpaVendorAdapter(adapter);
			factoryBean.setDataSource(dataSource);
			factoryBean.setPersistenceUnitName("manually-configured");
			Map<String, Object> properties = new HashMap<String, Object>();
			properties.put("configured", "manually");
			properties.put("hibernate.transaction.jta.platform", NoJtaPlatform.INSTANCE);
			factoryBean.setJpaPropertyMap(properties);
			factoryBean.afterPropertiesSet();
			return factoryBean.getObject();
		}

		@Bean
		public PlatformTransactionManager transactionManager(EntityManagerFactory emf) {
			JpaTransactionManager transactionManager = new JpaTransactionManager();
			transactionManager.setEntityManagerFactory(emf);
			return transactionManager;
		}

	}

	@Configuration
	@TestAutoConfigurationPackage(City.class)
	protected static class TestConfigurationWithTransactionManager {

		@Bean
		public PlatformTransactionManager transactionManager() {
			return new CustomJpaTransactionManager();
		}

	}

	@Configuration
	@TestAutoConfigurationPackage(AbstractJpaAutoConfigurationTests.class)
	public static class TestConfigurationWithCustomPersistenceUnitManager {

		private final DataSource dataSource;

		public TestConfigurationWithCustomPersistenceUnitManager(DataSource dataSource) {
			this.dataSource = dataSource;
		}

		@Bean
		public PersistenceUnitManager persistenceUnitManager() {
			DefaultPersistenceUnitManager persistenceUnitManager = new DefaultPersistenceUnitManager();
			persistenceUnitManager.setDefaultDataSource(this.dataSource);
			persistenceUnitManager.setPackagesToScan(City.class.getPackage().getName());
			return persistenceUnitManager;
		}

	}

	@SuppressWarnings("serial")
	static class CustomJpaTransactionManager extends JpaTransactionManager {

	}

}
