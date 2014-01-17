/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.autoconfigure.orm.jpa;

import java.util.Collections;
import java.util.Map;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Test;
import org.springframework.boot.autoconfigure.TestAutoConfigurationPackage;
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.EmbeddedDataSourceConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.test.City;
import org.springframework.boot.test.EnvironmentTestUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.support.OpenEntityManagerInViewFilter;
import org.springframework.orm.jpa.support.OpenEntityManagerInViewInterceptor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Base for JPA tests and tests for {@link JpaBaseConfiguration}.
 * 
 * @author Phillip Webb
 * @author Dave Syer
 */
public abstract class AbstractJpaAutoConfigurationTests {

	protected AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

	@After
	public void close() {
		this.context.close();
	}

	protected abstract Class<?> getAutoConfigureClass();

	@Test
	public void testEntityManagerCreated() throws Exception {
		setupTestConfiguration();
		this.context.refresh();
		assertNotNull(this.context.getBean(DataSource.class));
		assertNotNull(this.context.getBean(JpaTransactionManager.class));
	}

	@Test
	public void testDataSourceTransactionManagerNotCreated() throws Exception {
		this.context.register(DataSourceTransactionManagerAutoConfiguration.class);
		setupTestConfiguration();
		this.context.refresh();
		assertNotNull(this.context.getBean(DataSource.class));
		assertTrue(this.context.getBean("transactionManager") instanceof JpaTransactionManager);
	}

	@Test
	public void testOpenEntityManagerInViewInterceptorCreated() throws Exception {
		AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
		context.register(TestConfiguration.class, EmbeddedDataSourceConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class, getAutoConfigureClass());
		context.refresh();
		assertNotNull(context.getBean(OpenEntityManagerInViewInterceptor.class));
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
		assertEquals(0, getInterceptorBeans(context).length);
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
		assertEquals(0, getInterceptorBeans(context).length);
		context.close();
	}

	@Test
	public void customJpaProperties() throws Exception {
		EnvironmentTestUtils.addEnvironment(this.context, "spring.jpa.properties.a:b",
				"spring.jpa.properties.c:d");
		setupTestConfiguration();
		this.context.refresh();
		LocalContainerEntityManagerFactoryBean bean = this.context
				.getBean(LocalContainerEntityManagerFactoryBean.class);
		Map<String, Object> map = bean.getJpaPropertyMap();
		assertThat(map.get("a"), equalTo((Object) "b"));
		assertThat(map.get("c"), equalTo((Object) "d"));
	}

	@Test
	public void usesManuallyDefinedEntityManagerFactoryBeanIfAvailable() {
		setupTestConfiguration(TestConfigurationWithEntityManagerFactory.class);
		this.context.refresh();
		LocalContainerEntityManagerFactoryBean factoryBean = this.context
				.getBean(LocalContainerEntityManagerFactoryBean.class);
		Map<String, Object> map = factoryBean.getJpaPropertyMap();
		assertThat(map.get("configured"), equalTo((Object) "manually"));
	}

	@Test
	public void usesManuallyDefinedTransactionManagerBeanIfAvailable() {
		setupTestConfiguration(TestConfigurationWithTransactionManager.class);
		this.context.refresh();
		PlatformTransactionManager txManager = this.context
				.getBean(PlatformTransactionManager.class);
		assertThat(txManager, instanceOf(CustomJpaTransactionManager.class));
	}

	protected void setupTestConfiguration() {
		setupTestConfiguration(TestConfiguration.class);
	}

	protected void setupTestConfiguration(Class<?> configClass) {
		this.context.register(configClass, EmbeddedDataSourceConfiguration.class,
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
	protected static class TestConfigurationWithEntityManagerFactory extends
			TestConfiguration {

		@Bean
		public LocalContainerEntityManagerFactoryBean entityManagerFactory(
				DataSource dataSource, JpaVendorAdapter adapter) {

			LocalContainerEntityManagerFactoryBean factoryBean = new LocalContainerEntityManagerFactoryBean();
			factoryBean.setJpaVendorAdapter(adapter);
			factoryBean.setDataSource(dataSource);
			factoryBean.setPersistenceUnitName("manually-configured");
			factoryBean.setJpaPropertyMap(Collections.singletonMap("configured",
					"manually"));
			return factoryBean;
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

	static class CustomJpaTransactionManager extends JpaTransactionManager {
	}

}
