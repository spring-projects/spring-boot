/*
 * Copyright 2012-2023 the original author or authors.
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

import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import jakarta.persistence.EntityManagerFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.autoconfigure.domain.EntityScanPackages;
import org.springframework.boot.autoconfigure.transaction.TransactionManagerCustomizers;
import org.springframework.boot.autoconfigure.web.servlet.ConditionalOnMissingFilterBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ResourceLoader;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
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
 * @since 1.0.0
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(JpaProperties.class)
public abstract class JpaBaseConfiguration {

	private final DataSource dataSource;

	private final JpaProperties properties;

	private final JtaTransactionManager jtaTransactionManager;

	/**
     * Constructs a new JpaBaseConfiguration with the specified dataSource, properties, and jtaTransactionManager.
     * 
     * @param dataSource the DataSource to be used for JPA configuration
     * @param properties the JpaProperties to be used for JPA configuration
     * @param jtaTransactionManager the JtaTransactionManager to be used for JPA configuration, or null if not available
     */
    protected JpaBaseConfiguration(DataSource dataSource, JpaProperties properties,
			ObjectProvider<JtaTransactionManager> jtaTransactionManager) {
		this.dataSource = dataSource;
		this.properties = properties;
		this.jtaTransactionManager = jtaTransactionManager.getIfAvailable();
	}

	/**
     * Creates a new {@link PlatformTransactionManager} bean if no other bean of type {@link TransactionManager} is present.
     * 
     * @param transactionManagerCustomizers the customizers for the transaction manager
     * @return the created transaction manager
     */
    @Bean
	@ConditionalOnMissingBean(TransactionManager.class)
	public PlatformTransactionManager transactionManager(
			ObjectProvider<TransactionManagerCustomizers> transactionManagerCustomizers) {
		JpaTransactionManager transactionManager = new JpaTransactionManager();
		transactionManagerCustomizers
			.ifAvailable((customizers) -> customizers.customize((TransactionManager) transactionManager));
		return transactionManager;
	}

	/**
     * Create and configure the JpaVendorAdapter bean.
     * This bean is conditional on the absence of any existing JpaVendorAdapter bean.
     * 
     * @return the configured JpaVendorAdapter bean
     */
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

	/**
     * Create an instance of {@link EntityManagerFactoryBuilder} if no bean of the same type exists.
     * 
     * @param jpaVendorAdapter the JPA vendor adapter
     * @param persistenceUnitManager the persistence unit manager
     * @param customizers the customizers for the entity manager factory builder
     * @return the entity manager factory builder
     */
    @Bean
	@ConditionalOnMissingBean
	public EntityManagerFactoryBuilder entityManagerFactoryBuilder(JpaVendorAdapter jpaVendorAdapter,
			ObjectProvider<PersistenceUnitManager> persistenceUnitManager,
			ObjectProvider<EntityManagerFactoryBuilderCustomizer> customizers) {
		EntityManagerFactoryBuilder builder = new EntityManagerFactoryBuilder(jpaVendorAdapter,
				this.properties.getProperties(), persistenceUnitManager.getIfAvailable());
		customizers.orderedStream().forEach((customizer) -> customizer.customize(builder));
		return builder;
	}

	/**
     * Creates and configures the entity manager factory bean.
     * 
     * @param factoryBuilder The entity manager factory builder.
     * @param persistenceManagedTypes The persistence managed types.
     * @return The configured entity manager factory bean.
     */
    @Bean
	@Primary
	@ConditionalOnMissingBean({ LocalContainerEntityManagerFactoryBean.class, EntityManagerFactory.class })
	public LocalContainerEntityManagerFactoryBean entityManagerFactory(EntityManagerFactoryBuilder factoryBuilder,
			PersistenceManagedTypes persistenceManagedTypes) {
		Map<String, Object> vendorProperties = getVendorProperties();
		customizeVendorProperties(vendorProperties);
		return factoryBuilder.dataSource(this.dataSource)
			.managedTypes(persistenceManagedTypes)
			.properties(vendorProperties)
			.mappingResources(getMappingResources())
			.jta(isJta())
			.build();
	}

	/**
     * Creates a JPA vendor adapter.
     *
     * @return the JPA vendor adapter
     */
    protected abstract AbstractJpaVendorAdapter createJpaVendorAdapter();

	/**
     * Retrieves the vendor-specific properties for the JPA configuration.
     *
     * @return a map containing the vendor-specific properties
     */
    protected abstract Map<String, Object> getVendorProperties();

	/**
	 * Customize vendor properties before they are used. Allows for post-processing (for
	 * example to configure JTA specific settings).
	 * @param vendorProperties the vendor properties to customize
	 */
	protected void customizeVendorProperties(Map<String, Object> vendorProperties) {
	}

	/**
     * Retrieves the mapping resources from the properties and converts them into an array of strings.
     * 
     * @return an array of strings representing the mapping resources, or null if the mapping resources are empty
     */
    private String[] getMappingResources() {
		List<String> mappingResources = this.properties.getMappingResources();
		return (!ObjectUtils.isEmpty(mappingResources) ? StringUtils.toStringArray(mappingResources) : null);
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

	/**
     * PersistenceManagedTypesConfiguration class.
     */
    @Configuration(proxyBeanMethods = false)
	@ConditionalOnMissingBean({ LocalContainerEntityManagerFactoryBean.class, EntityManagerFactory.class })
	static class PersistenceManagedTypesConfiguration {

		/**
         * This method is responsible for creating and returning an instance of PersistenceManagedTypes.
         * It uses a PersistenceManagedTypesScanner to scan the specified packages and identify the managed types.
         * The packages to scan are determined by calling the getPackagesToScan method with the provided beanFactory.
         * The resourceLoader is used to load the resources required for scanning.
         * 
         * @param beanFactory The bean factory used to determine the packages to scan.
         * @param resourceLoader The resource loader used to load the resources required for scanning.
         * @return An instance of PersistenceManagedTypes containing the scanned managed types.
         */
        @Bean
		@Primary
		@ConditionalOnMissingBean
		static PersistenceManagedTypes persistenceManagedTypes(BeanFactory beanFactory, ResourceLoader resourceLoader) {
			String[] packagesToScan = getPackagesToScan(beanFactory);
			return new PersistenceManagedTypesScanner(resourceLoader).scan(packagesToScan);
		}

		/**
         * Retrieves the packages to scan for entity classes.
         * 
         * @param beanFactory the BeanFactory to retrieve the packages from
         * @return an array of package names to scan
         */
        private static String[] getPackagesToScan(BeanFactory beanFactory) {
			List<String> packages = EntityScanPackages.get(beanFactory).getPackageNames();
			if (packages.isEmpty() && AutoConfigurationPackages.has(beanFactory)) {
				packages = AutoConfigurationPackages.get(beanFactory);
			}
			return StringUtils.toStringArray(packages);
		}

	}

	/**
     * JpaWebConfiguration class.
     */
    @Configuration(proxyBeanMethods = false)
	@ConditionalOnWebApplication(type = Type.SERVLET)
	@ConditionalOnClass(WebMvcConfigurer.class)
	@ConditionalOnMissingBean({ OpenEntityManagerInViewInterceptor.class, OpenEntityManagerInViewFilter.class })
	@ConditionalOnMissingFilterBean(OpenEntityManagerInViewFilter.class)
	@ConditionalOnProperty(prefix = "spring.jpa", name = "open-in-view", havingValue = "true", matchIfMissing = true)
	protected static class JpaWebConfiguration {

		private static final Log logger = LogFactory.getLog(JpaWebConfiguration.class);

		private final JpaProperties jpaProperties;

		/**
         * Constructs a new JpaWebConfiguration with the specified JpaProperties.
         * 
         * @param jpaProperties the JpaProperties to be used for configuring JPA.
         */
        protected JpaWebConfiguration(JpaProperties jpaProperties) {
			this.jpaProperties = jpaProperties;
		}

		/**
         * Creates and returns an instance of OpenEntityManagerInViewInterceptor.
         * 
         * This interceptor is responsible for keeping the EntityManager open during view rendering.
         * If the "spring.jpa.open-in-view" property is not explicitly configured, a warning message will be logged.
         * 
         * @return the OpenEntityManagerInViewInterceptor instance
         */
        @Bean
		public OpenEntityManagerInViewInterceptor openEntityManagerInViewInterceptor() {
			if (this.jpaProperties.getOpenInView() == null) {
				logger.warn("spring.jpa.open-in-view is enabled by default. "
						+ "Therefore, database queries may be performed during view "
						+ "rendering. Explicitly configure spring.jpa.open-in-view to disable this warning");
			}
			return new OpenEntityManagerInViewInterceptor();
		}

		/**
         * Configures the OpenEntityManagerInViewInterceptor as a web request interceptor.
         * 
         * @param interceptor The OpenEntityManagerInViewInterceptor to be added as an interceptor.
         * @return The WebMvcConfigurer object with the added interceptor.
         */
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
