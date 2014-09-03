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

import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.persistenceunit.PersistenceUnitManager;
import org.springframework.orm.jpa.support.OpenEntityManagerInViewFilter;
import org.springframework.orm.jpa.support.OpenEntityManagerInViewInterceptor;
import org.springframework.orm.jpa.vendor.AbstractJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.jta.JtaTransactionManager;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

/**
 * Base {@link EnableAutoConfiguration Auto-configuration} for JPA.
 *
 * @author Phillip Webb
 * @author Dave Syer
 * @author Oliver Gierke
 */
@EnableConfigurationProperties(JpaProperties.class)
@Import(DataSourceInitializedPublisher.Registrar.class)
public abstract class JpaBaseConfiguration implements BeanFactoryAware {

	private static final String[] NO_PACKAGES = new String[0];

	private ConfigurableListableBeanFactory beanFactory;

	@Autowired
	private DataSource dataSource;

	@Autowired(required = false)
	private PersistenceUnitManager persistenceUnitManager;

	@Autowired
	private JpaProperties jpaProperties;

	@Autowired(required = false)
	private JtaTransactionManager jtaTransactionManager;

	@Bean
	@ConditionalOnMissingBean(PlatformTransactionManager.class)
	public PlatformTransactionManager transactionManager() {
		return new JpaTransactionManager();
	}

	@Bean
	@ConditionalOnMissingBean
	public JpaVendorAdapter jpaVendorAdapter() {
		AbstractJpaVendorAdapter adapter = createJpaVendorAdapter();
		adapter.setShowSql(this.jpaProperties.isShowSql());
		adapter.setDatabase(this.jpaProperties.getDatabase());
		adapter.setDatabasePlatform(this.jpaProperties.getDatabasePlatform());
		adapter.setGenerateDdl(this.jpaProperties.isGenerateDdl());
		return adapter;
	}

	@Bean
	@ConditionalOnMissingBean
	public EntityManagerFactoryBuilder entityManagerFactoryBuilder(
			JpaVendorAdapter jpaVendorAdapter) {
		EntityManagerFactoryBuilder builder = new EntityManagerFactoryBuilder(
				jpaVendorAdapter, this.jpaProperties, this.persistenceUnitManager);
		builder.setCallback(getVendorCallback());
		return builder;
	}

	@Bean
	@Primary
	@ConditionalOnMissingBean
	public LocalContainerEntityManagerFactoryBean entityManagerFactory(
			EntityManagerFactoryBuilder factoryBuilder) {
		Map<String, Object> vendorProperties = getVendorProperties();
		customizeVendorProperties(vendorProperties);
		return factoryBuilder.dataSource(this.dataSource).packages(getPackagesToScan())
				.properties(vendorProperties).jta(isJta()).build();
	}

	protected abstract AbstractJpaVendorAdapter createJpaVendorAdapter();

	protected abstract Map<String, Object> getVendorProperties();

	/**
	 * Customize vendor properties before they are used. Allows for post processing (for
	 * example to configure JTA specific settings).
	 * @param vendorProperties the vendor properties to customize
	 */
	protected void customizeVendorProperties(Map<String, Object> vendorProperties) {
	}

	protected EntityManagerFactoryBuilder.EntityManagerFactoryBeanCallback getVendorCallback() {
		return null;
	}

	protected String[] getPackagesToScan() {
		if (AutoConfigurationPackages.has(this.beanFactory)) {
			List<String> basePackages = AutoConfigurationPackages.get(this.beanFactory);
			return basePackages.toArray(new String[basePackages.size()]);
		}
		return NO_PACKAGES;
	}

	protected void configure(
			LocalContainerEntityManagerFactoryBean entityManagerFactoryBean) {
	}

	/**
	 * @return the jtaTransactionManager or {@code null}
	 */
	protected JtaTransactionManager getJtaTransactionManager() {
		return this.jtaTransactionManager;
	}

	/**
	 * Returns if a JTA {@link PlatformTransactionManager} is being used.
	 */
	protected final boolean isJta() {
		return (this.jtaTransactionManager != null);
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = (ConfigurableListableBeanFactory) beanFactory;
	}

	@Configuration
	@ConditionalOnWebApplication
	@ConditionalOnMissingBean({ OpenEntityManagerInViewInterceptor.class,
			OpenEntityManagerInViewFilter.class })
	@ConditionalOnProperty(prefix = "spring.jpa", name = "open-in-view", havingValue = "true", matchIfMissing = true)
	protected static class JpaWebConfiguration extends WebMvcConfigurerAdapter {

		@Override
		public void addInterceptors(InterceptorRegistry registry) {
			registry.addWebRequestInterceptor(openEntityManagerInViewInterceptor());
		}

		@Bean
		public OpenEntityManagerInViewInterceptor openEntityManagerInViewInterceptor() {
			return new OpenEntityManagerInViewInterceptor();
		}

	}

}
