/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.jpa.autoconfigure;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import jakarta.persistence.EntityManagerFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingFilterBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jpa.EntityManagerFactoryBuilder;
import org.springframework.boot.persistence.autoconfigure.EntityScanPackages;
import org.springframework.boot.transaction.autoconfigure.TransactionManagerCustomizers;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ResourceLoader;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.persistenceunit.ManagedClassNameFilter;
import org.springframework.orm.jpa.persistenceunit.PersistenceManagedTypes;
import org.springframework.orm.jpa.persistenceunit.PersistenceManagedTypesScanner;
import org.springframework.orm.jpa.persistenceunit.PersistenceUnitManager;
import org.springframework.orm.jpa.support.OpenEntityManagerInViewFilter;
import org.springframework.orm.jpa.support.OpenEntityManagerInViewInterceptor;
import org.springframework.orm.jpa.vendor.AbstractJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionManager;
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
 * @author Yanming Zhou
 * @since 4.0.0
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(JpaProperties.class)
public abstract class JpaBaseConfiguration {

	private final DataSource dataSource;

	private final JpaProperties properties;

	private final @Nullable JtaTransactionManager jtaTransactionManager;

	protected JpaBaseConfiguration(DataSource dataSource, JpaProperties properties,
			ObjectProvider<JtaTransactionManager> jtaTransactionManager) {
		this.dataSource = dataSource;
		this.properties = properties;
		this.jtaTransactionManager = jtaTransactionManager.getIfAvailable();
	}

	@Bean
	@ConditionalOnMissingBean(TransactionManager.class)
	public PlatformTransactionManager transactionManager(
			ObjectProvider<TransactionManagerCustomizers> transactionManagerCustomizers) {
		JpaTransactionManager transactionManager = new JpaTransactionManager();
		transactionManagerCustomizers.ifAvailable((customizers) -> customizers.customize(transactionManager));
		return transactionManager;
	}

	@Bean
	@ConditionalOnMissingBean
	public JpaVendorAdapter jpaVendorAdapter() {
		AbstractJpaVendorAdapter adapter = createJpaVendorAdapter();
		adapter.setShowSql(this.properties.isShowSql());
		if (this.properties.getDatabase() != null) {
			adapter.setDatabase(this.properties.getDatabase());
		}
		if (this.properties.getDatabasePlatform() != null) {
			adapter.setDatabasePlatform(this.properties.getDatabasePlatform());
		}
		adapter.setGenerateDdl(this.properties.isGenerateDdl());
		return adapter;
	}

	@Bean
	@ConditionalOnMissingBean
	public EntityManagerFactoryBuilder entityManagerFactoryBuilder(JpaVendorAdapter jpaVendorAdapter,
			ObjectProvider<PersistenceUnitManager> persistenceUnitManager,
			ObjectProvider<EntityManagerFactoryBuilderCustomizer> customizers) {
		EntityManagerFactoryBuilder builder = new EntityManagerFactoryBuilder(jpaVendorAdapter,
				this::buildJpaProperties, persistenceUnitManager.getIfAvailable());
		customizers.orderedStream().forEach((customizer) -> customizer.customize(builder));
		return builder;
	}

	private Map<String, ?> buildJpaProperties(DataSource dataSource) {
		Map<String, Object> properties = new HashMap<>(this.properties.getProperties());
		Map<String, Object> vendorProperties = getVendorProperties(dataSource);
		customizeVendorProperties(vendorProperties);
		properties.putAll(vendorProperties);
		return properties;
	}

	@Bean
	@Primary
	@ConditionalOnMissingBean({ LocalContainerEntityManagerFactoryBean.class, EntityManagerFactory.class })
	public LocalContainerEntityManagerFactoryBean entityManagerFactory(EntityManagerFactoryBuilder factoryBuilder,
			PersistenceManagedTypes persistenceManagedTypes) {
		return factoryBuilder.dataSource(this.dataSource)
			.managedTypes(persistenceManagedTypes)
			.mappingResources(getMappingResources())
			.jta(isJta())
			.build();
	}

	protected abstract AbstractJpaVendorAdapter createJpaVendorAdapter();

	/**
	 * Return the vendor-specific properties for the given {@link DataSource}.
	 * @param dataSource the data source
	 * @return the vendor properties
	 */
	protected abstract Map<String, Object> getVendorProperties(DataSource dataSource);

	/**
	 * Customize vendor properties before they are used. Allows for post-processing (for
	 * example to configure JTA specific settings).
	 * @param vendorProperties the vendor properties to customize
	 */
	protected void customizeVendorProperties(Map<String, Object> vendorProperties) {
	}

	private String @Nullable [] getMappingResources() {
		List<String> mappingResources = this.properties.getMappingResources();
		return (!ObjectUtils.isEmpty(mappingResources) ? StringUtils.toStringArray(mappingResources) : null);
	}

	/**
	 * Return the JTA transaction manager.
	 * @return the transaction manager or {@code null}
	 */
	protected @Nullable JtaTransactionManager getJtaTransactionManager() {
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

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnMissingBean({ LocalContainerEntityManagerFactoryBean.class, EntityManagerFactory.class })
	static class PersistenceManagedTypesConfiguration {

		@Bean
		@Primary
		@ConditionalOnMissingBean
		static PersistenceManagedTypes persistenceManagedTypes(BeanFactory beanFactory, ResourceLoader resourceLoader,
				ObjectProvider<ManagedClassNameFilter> managedClassNameFilter) {
			String[] packagesToScan = getPackagesToScan(beanFactory);
			return new PersistenceManagedTypesScanner(resourceLoader, managedClassNameFilter.getIfAvailable())
				.scan(packagesToScan);
		}

		private static String[] getPackagesToScan(BeanFactory beanFactory) {
			List<String> packages = EntityScanPackages.get(beanFactory).getPackageNames();
			if (packages.isEmpty() && AutoConfigurationPackages.has(beanFactory)) {
				packages = AutoConfigurationPackages.get(beanFactory);
			}
			return StringUtils.toStringArray(packages);
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnWebApplication(type = Type.SERVLET)
	@ConditionalOnClass(WebMvcConfigurer.class)
	@ConditionalOnMissingBean({ OpenEntityManagerInViewInterceptor.class, OpenEntityManagerInViewFilter.class })
	@ConditionalOnMissingFilterBean(OpenEntityManagerInViewFilter.class)
	@ConditionalOnBooleanProperty(name = "spring.jpa.open-in-view", matchIfMissing = true)
	protected static class JpaWebConfiguration {

		private static final Log logger = LogFactory.getLog(JpaWebConfiguration.class);

		private final JpaProperties jpaProperties;

		protected JpaWebConfiguration(JpaProperties jpaProperties) {
			this.jpaProperties = jpaProperties;
		}

		@Bean
		public OpenEntityManagerInViewInterceptor openEntityManagerInViewInterceptor() {
			if (this.jpaProperties.getOpenInView() == null) {
				logger.warn("spring.jpa.open-in-view is enabled by default. "
						+ "Therefore, database queries may be performed during view "
						+ "rendering. Explicitly configure spring.jpa.open-in-view to disable this warning");
			}
			return new OpenEntityManagerInViewInterceptor();
		}

		@Bean
		public WebMvcConfigurer openEntityManagerInViewInterceptorConfigurer(
				OpenEntityManagerInViewInterceptor interceptor) {
			return new WebMvcConfigurer() {

				@Override
				public void addInterceptors(InterceptorRegistry registry) {
					registry.addWebRequestInterceptor(interceptor);
				}

			};
		}

	}

}
