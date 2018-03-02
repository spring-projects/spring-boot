/*
 * Copyright 2012-2018 the original author or authors.
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

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.autoconfigure.domain.EntityScanPackages;
import org.springframework.boot.autoconfigure.transaction.TransactionManagerCustomizers;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
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
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Base {@link EnableAutoConfiguration Auto-configuration} for JPA.
 *
 * @author Phillip Webb
 * @author Dave Syer
 * @author Oliver Gierke
 * @author Andy Wilkinson
 * @author Kazuki Shimizu
 * @author Eddú Meléndez
 */
@EnableConfigurationProperties(JpaProperties.class)
@Import(DataSourceInitializedPublisher.Registrar.class)
public abstract class JpaBaseConfiguration implements BeanFactoryAware {

	private final DataSource dataSource;

	private final JpaProperties properties;

	private final JtaTransactionManager jtaTransactionManager;

	private final TransactionManagerCustomizers transactionManagerCustomizers;

	private ConfigurableListableBeanFactory beanFactory;

	protected JpaBaseConfiguration(DataSource dataSource, JpaProperties properties,
			ObjectProvider<JtaTransactionManager> jtaTransactionManager,
			ObjectProvider<TransactionManagerCustomizers> transactionManagerCustomizers) {
		this.dataSource = dataSource;
		this.properties = properties;
		this.jtaTransactionManager = jtaTransactionManager.getIfAvailable();
		this.transactionManagerCustomizers = transactionManagerCustomizers
				.getIfAvailable();
	}

	@Bean
	@ConditionalOnMissingBean(PlatformTransactionManager.class)
	public PlatformTransactionManager transactionManager() {
		JpaTransactionManager transactionManager = new JpaTransactionManager();
		if (this.transactionManagerCustomizers != null) {
			this.transactionManagerCustomizers.customize(transactionManager);
		}
		return transactionManager;
	}

	@Bean
	@ConditionalOnMissingBean
	public JpaVendorAdapter jpaVendorAdapter() {
		AbstractJpaVendorAdapter adapter = createJpaVendorAdapter();
		adapter.setShowSql(this.properties.isShowSql());
		adapter.setDatabase(this.properties.determineDatabase(this.dataSource));
		adapter.setDatabasePlatform(this.properties.getDatabasePlatform());
		adapter.setGenerateDdl(this.properties.isGenerateDdl());
		return adapter;
	}

	@Bean
	@ConditionalOnMissingBean
	public EntityManagerFactoryBuilder entityManagerFactoryBuilder(
			JpaVendorAdapter jpaVendorAdapter,
			ObjectProvider<PersistenceUnitManager> persistenceUnitManager) {
		EntityManagerFactoryBuilder builder = new EntityManagerFactoryBuilder(
				jpaVendorAdapter, this.properties.getProperties(),
				persistenceUnitManager.getIfAvailable());
		builder.setCallback(getVendorCallback());
		return builder;
	}

	@Bean
	@Primary
	@ConditionalOnMissingBean({ LocalContainerEntityManagerFactoryBean.class,
			EntityManagerFactory.class })
	public LocalContainerEntityManagerFactoryBean entityManagerFactory(
			EntityManagerFactoryBuilder factoryBuilder) {
		Map<String, Object> vendorProperties = getVendorProperties();
		customizeVendorProperties(vendorProperties);
		return factoryBuilder.dataSource(this.dataSource).packages(getPackagesToScan())
				.properties(vendorProperties).mappingResources(getMappingResources())
				.jta(isJta()).build();
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
		List<String> packages = EntityScanPackages.get(this.beanFactory)
				.getPackageNames();
		if (packages.isEmpty() && AutoConfigurationPackages.has(this.beanFactory)) {
			packages = AutoConfigurationPackages.get(this.beanFactory);
		}
		return StringUtils.toStringArray(packages);
	}

	private String[] getMappingResources() {
		List<String> mappingResources = this.properties.getMappingResources();
		return (!ObjectUtils.isEmpty(mappingResources)
				? StringUtils.toStringArray(mappingResources) : null);
	}

	/**
	 * Return the JTA transaction manager.
	 * @return the transaction manager or {@code null}
	 */
	protected JtaTransactionManager getJtaTransactionManager() {
		return this.jtaTransactionManager;
	}

	/**
	 * Returns if a JTA {@link PlatformTransactionManager} is being used.
	 * @return if a JTA transaction manager is being used
	 */
	protected final boolean isJta() {
		return (this.jtaTransactionManager != null);
	}

	/**
	 * Return the {@link JpaProperties}.
	 * @return the properties
	 */
	protected final JpaProperties getProperties() {
		return this.properties;
	}

	/**
	 * Return the {@link DataSource}.
	 * @return the data source
	 */
	protected final DataSource getDataSource() {
		return this.dataSource;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = (ConfigurableListableBeanFactory) beanFactory;
	}

	@Configuration
	@ConditionalOnWebApplication(type = Type.SERVLET)
	@ConditionalOnClass(WebMvcConfigurer.class)
	@ConditionalOnMissingBean({ OpenEntityManagerInViewInterceptor.class,
			OpenEntityManagerInViewFilter.class })
	@ConditionalOnProperty(prefix = "spring.jpa", name = "open-in-view", havingValue = "true", matchIfMissing = true)
	protected static class JpaWebConfiguration {

		// Defined as a nested config to ensure WebMvcConfigurerAdapter is not read when
		// not on the classpath
		@Configuration
		protected static class JpaWebMvcConfiguration implements WebMvcConfigurer {

			private static final Log logger = LogFactory
					.getLog(JpaWebMvcConfiguration.class);

			private final JpaProperties jpaProperties;

			protected JpaWebMvcConfiguration(JpaProperties jpaProperties) {
				this.jpaProperties = jpaProperties;
			}

			@Bean
			public OpenEntityManagerInViewInterceptor openEntityManagerInViewInterceptor() {
				if (this.jpaProperties.getOpenInView() == null) {
					logger.warn("spring.jpa.open-in-view is enabled by default. "
							+ "Therefore, database queries may be performed during view "
							+ "rendering. Explicitly configure "
							+ "spring.jpa.open-in-view to disable this warning");
				}
				return new OpenEntityManagerInViewInterceptor();
			}

			@Override
			public void addInterceptors(InterceptorRegistry registry) {
				registry.addWebRequestInterceptor(openEntityManagerInViewInterceptor());
			}

		}

	}

}
