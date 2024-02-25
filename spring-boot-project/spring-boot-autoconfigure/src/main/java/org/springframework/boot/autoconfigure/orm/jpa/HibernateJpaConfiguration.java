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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy;
import org.hibernate.boot.model.naming.ImplicitNamingStrategy;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.cfg.AvailableSettings;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeHint;
import org.springframework.aot.hint.TypeHint.Builder;
import org.springframework.aot.hint.TypeReference;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaConfiguration.HibernateRuntimeHints;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jdbc.SchemaManagementProvider;
import org.springframework.boot.jdbc.metadata.CompositeDataSourcePoolMetadataProvider;
import org.springframework.boot.jdbc.metadata.DataSourcePoolMetadata;
import org.springframework.boot.jdbc.metadata.DataSourcePoolMetadataProvider;
import org.springframework.boot.orm.jpa.hibernate.SpringImplicitNamingStrategy;
import org.springframework.boot.orm.jpa.hibernate.SpringJtaPlatform;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.jndi.JndiLocatorDelegate;
import org.springframework.orm.hibernate5.SpringBeanContainer;
import org.springframework.orm.jpa.vendor.AbstractJpaVendorAdapter;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.jta.JtaTransactionManager;
import org.springframework.util.ClassUtils;

/**
 * {@link JpaBaseConfiguration} implementation for Hibernate.
 *
 * @author Phillip Webb
 * @author Josh Long
 * @author Manuel Doninger
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @author Moritz Halbritter
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(HibernateProperties.class)
@ConditionalOnSingleCandidate(DataSource.class)
@ImportRuntimeHints(HibernateRuntimeHints.class)
class HibernateJpaConfiguration extends JpaBaseConfiguration {

	private static final Log logger = LogFactory.getLog(HibernateJpaConfiguration.class);

	private static final String JTA_PLATFORM = "hibernate.transaction.jta.platform";

	private static final String PROVIDER_DISABLES_AUTOCOMMIT = "hibernate.connection.provider_disables_autocommit";

	/**
	 * {@code NoJtaPlatform} implementations for various Hibernate versions.
	 */
	private static final String[] NO_JTA_PLATFORM_CLASSES = {
			"org.hibernate.engine.transaction.jta.platform.internal.NoJtaPlatform",
			"org.hibernate.service.jta.platform.internal.NoJtaPlatform" };

	private final HibernateProperties hibernateProperties;

	private final HibernateDefaultDdlAutoProvider defaultDdlAutoProvider;

	private final DataSourcePoolMetadataProvider poolMetadataProvider;

	private final List<HibernatePropertiesCustomizer> hibernatePropertiesCustomizers;

	/**
     * Constructs a new HibernateJpaConfiguration object with the specified parameters.
     * 
     * @param dataSource The data source to be used for the JPA configuration.
     * @param jpaProperties The JPA properties to be used for the configuration.
     * @param beanFactory The bean factory to be used for the configuration.
     * @param jtaTransactionManager The JTA transaction manager to be used for the configuration.
     * @param hibernateProperties The Hibernate properties to be used for the configuration.
     * @param metadataProviders The metadata providers to be used for the configuration.
     * @param providers The schema management providers to be used for the configuration.
     * @param physicalNamingStrategy The physical naming strategy to be used for the configuration.
     * @param implicitNamingStrategy The implicit naming strategy to be used for the configuration.
     * @param hibernatePropertiesCustomizers The Hibernate properties customizers to be used for the configuration.
     */
    HibernateJpaConfiguration(DataSource dataSource, JpaProperties jpaProperties,
			ConfigurableListableBeanFactory beanFactory, ObjectProvider<JtaTransactionManager> jtaTransactionManager,
			HibernateProperties hibernateProperties,
			ObjectProvider<Collection<DataSourcePoolMetadataProvider>> metadataProviders,
			ObjectProvider<SchemaManagementProvider> providers,
			ObjectProvider<PhysicalNamingStrategy> physicalNamingStrategy,
			ObjectProvider<ImplicitNamingStrategy> implicitNamingStrategy,
			ObjectProvider<HibernatePropertiesCustomizer> hibernatePropertiesCustomizers) {
		super(dataSource, jpaProperties, jtaTransactionManager);
		this.hibernateProperties = hibernateProperties;
		this.defaultDdlAutoProvider = new HibernateDefaultDdlAutoProvider(providers);
		this.poolMetadataProvider = new CompositeDataSourcePoolMetadataProvider(metadataProviders.getIfAvailable());
		this.hibernatePropertiesCustomizers = determineHibernatePropertiesCustomizers(
				physicalNamingStrategy.getIfAvailable(), implicitNamingStrategy.getIfAvailable(), beanFactory,
				hibernatePropertiesCustomizers.orderedStream().toList());
	}

	/**
     * Determines the list of HibernatePropertiesCustomizer to be applied.
     * 
     * @param physicalNamingStrategy The physical naming strategy to be used.
     * @param implicitNamingStrategy The implicit naming strategy to be used.
     * @param beanFactory The configurable listable bean factory.
     * @param hibernatePropertiesCustomizers The list of HibernatePropertiesCustomizer to be applied.
     * @return The list of HibernatePropertiesCustomizer determined.
     */
    private List<HibernatePropertiesCustomizer> determineHibernatePropertiesCustomizers(
			PhysicalNamingStrategy physicalNamingStrategy, ImplicitNamingStrategy implicitNamingStrategy,
			ConfigurableListableBeanFactory beanFactory,
			List<HibernatePropertiesCustomizer> hibernatePropertiesCustomizers) {
		List<HibernatePropertiesCustomizer> customizers = new ArrayList<>();
		if (ClassUtils.isPresent("org.hibernate.resource.beans.container.spi.BeanContainer",
				getClass().getClassLoader())) {
			customizers.add((properties) -> properties.put(AvailableSettings.BEAN_CONTAINER,
					new SpringBeanContainer(beanFactory)));
		}
		if (physicalNamingStrategy != null || implicitNamingStrategy != null) {
			customizers
				.add(new NamingStrategiesHibernatePropertiesCustomizer(physicalNamingStrategy, implicitNamingStrategy));
		}
		customizers.addAll(hibernatePropertiesCustomizers);
		return customizers;
	}

	/**
     * Creates a new instance of {@link HibernateJpaVendorAdapter} to be used as the JPA vendor adapter.
     * 
     * @return the newly created {@link HibernateJpaVendorAdapter}
     */
    @Override
	protected AbstractJpaVendorAdapter createJpaVendorAdapter() {
		return new HibernateJpaVendorAdapter();
	}

	/**
     * Retrieves the vendor properties for the Hibernate JPA configuration.
     * 
     * @return a map containing the vendor properties
     */
    @Override
	protected Map<String, Object> getVendorProperties() {
		Supplier<String> defaultDdlMode = () -> this.defaultDdlAutoProvider.getDefaultDdlAuto(getDataSource());
		return new LinkedHashMap<>(this.hibernateProperties.determineHibernateProperties(
				getProperties().getProperties(), new HibernateSettings().ddlAuto(defaultDdlMode)
					.hibernatePropertiesCustomizers(this.hibernatePropertiesCustomizers)));
	}

	/**
     * Customizes the vendor properties for the Hibernate JPA configuration.
     * 
     * @param vendorProperties the map of vendor properties to customize
     */
    @Override
	protected void customizeVendorProperties(Map<String, Object> vendorProperties) {
		super.customizeVendorProperties(vendorProperties);
		if (!vendorProperties.containsKey(JTA_PLATFORM)) {
			configureJtaPlatform(vendorProperties);
		}
		if (!vendorProperties.containsKey(PROVIDER_DISABLES_AUTOCOMMIT)) {
			configureProviderDisablesAutocommit(vendorProperties);
		}
	}

	/**
     * Configures the JTA platform for Hibernate.
     * 
     * @param vendorProperties the vendor-specific properties
     * @throws LinkageError if there is an error configuring the JTA platform
     */
    private void configureJtaPlatform(Map<String, Object> vendorProperties) throws LinkageError {
		JtaTransactionManager jtaTransactionManager = getJtaTransactionManager();
		// Make sure Hibernate doesn't attempt to auto-detect a JTA platform
		if (jtaTransactionManager == null) {
			vendorProperties.put(JTA_PLATFORM, getNoJtaPlatformManager());
		}
		// As of Hibernate 5.2, Hibernate can fully integrate with the WebSphere
		// transaction manager on its own.
		else if (!runningOnWebSphere()) {
			configureSpringJtaPlatform(vendorProperties, jtaTransactionManager);
		}
	}

	/**
     * Configures the provider to disable autocommit if the data source autocommit is disabled and JTA is not enabled.
     * 
     * @param vendorProperties the map of vendor-specific properties
     */
    private void configureProviderDisablesAutocommit(Map<String, Object> vendorProperties) {
		if (isDataSourceAutoCommitDisabled() && !isJta()) {
			vendorProperties.put(PROVIDER_DISABLES_AUTOCOMMIT, "true");
		}
	}

	/**
     * Checks if the auto-commit feature is disabled for the data source.
     * 
     * @return {@code true} if the auto-commit feature is disabled, {@code false} otherwise
     */
    private boolean isDataSourceAutoCommitDisabled() {
		DataSourcePoolMetadata poolMetadata = this.poolMetadataProvider.getDataSourcePoolMetadata(getDataSource());
		return poolMetadata != null && Boolean.FALSE.equals(poolMetadata.getDefaultAutoCommit());
	}

	/**
     * Checks if the application is running on WebSphere.
     * 
     * @return {@code true} if the application is running on WebSphere, {@code false} otherwise.
     */
    private boolean runningOnWebSphere() {
		return ClassUtils.isPresent("com.ibm.websphere.jtaextensions.ExtendedJTATransaction",
				getClass().getClassLoader());
	}

	/**
     * Configures the Spring JTA platform for Hibernate.
     * 
     * @param vendorProperties the vendor-specific properties
     * @param jtaTransactionManager the JTA transaction manager
     * @throws IllegalStateException if unable to set Hibernate JTA platform
     * @throws LinkageError if a LinkageError occurs while setting Hibernate JTA platform
     */
    private void configureSpringJtaPlatform(Map<String, Object> vendorProperties,
			JtaTransactionManager jtaTransactionManager) {
		try {
			vendorProperties.put(JTA_PLATFORM, new SpringJtaPlatform(jtaTransactionManager));
		}
		catch (LinkageError ex) {
			// NoClassDefFoundError can happen if Hibernate 4.2 is used and some
			// containers (e.g. JBoss EAP 6) wrap it in the superclass LinkageError
			if (!isUsingJndi()) {
				throw new IllegalStateException(
						"Unable to set Hibernate JTA platform, are you using the correct version of Hibernate?", ex);
			}
			// Assume that Hibernate will use JNDI
			if (logger.isDebugEnabled()) {
				logger.debug("Unable to set Hibernate JTA platform : " + ex.getMessage());
			}
		}
	}

	/**
     * Checks if the default JNDI environment is being used.
     * 
     * @return true if the default JNDI environment is available, false otherwise
     */
    private boolean isUsingJndi() {
		try {
			return JndiLocatorDelegate.isDefaultJndiEnvironmentAvailable();
		}
		catch (Error ex) {
			return false;
		}
	}

	/**
     * Retrieves the JtaPlatform manager that does not support JTA transactions.
     * 
     * @return The JtaPlatform manager instance.
     * @throws IllegalStateException if no available JtaPlatform candidates are found.
     */
    private Object getNoJtaPlatformManager() {
		for (String candidate : NO_JTA_PLATFORM_CLASSES) {
			try {
				return Class.forName(candidate).getDeclaredConstructor().newInstance();
			}
			catch (Exception ex) {
				// Continue searching
			}
		}
		throw new IllegalStateException(
				"No available JtaPlatform candidates amongst " + Arrays.toString(NO_JTA_PLATFORM_CLASSES));
	}

	/**
     * NamingStrategiesHibernatePropertiesCustomizer class.
     */
    private static class NamingStrategiesHibernatePropertiesCustomizer implements HibernatePropertiesCustomizer {

		private final PhysicalNamingStrategy physicalNamingStrategy;

		private final ImplicitNamingStrategy implicitNamingStrategy;

		/**
         * Constructs a new instance of NamingStrategiesHibernatePropertiesCustomizer with the specified physicalNamingStrategy and implicitNamingStrategy.
         * 
         * @param physicalNamingStrategy the physical naming strategy to be used
         * @param implicitNamingStrategy the implicit naming strategy to be used
         */
        NamingStrategiesHibernatePropertiesCustomizer(PhysicalNamingStrategy physicalNamingStrategy,
				ImplicitNamingStrategy implicitNamingStrategy) {
			this.physicalNamingStrategy = physicalNamingStrategy;
			this.implicitNamingStrategy = implicitNamingStrategy;
		}

		/**
         * Customizes the Hibernate properties by adding the physical and implicit naming strategies.
         * 
         * @param hibernateProperties the Hibernate properties to be customized
         */
        @Override
		public void customize(Map<String, Object> hibernateProperties) {
			if (this.physicalNamingStrategy != null) {
				hibernateProperties.put("hibernate.physical_naming_strategy", this.physicalNamingStrategy);
			}
			if (this.implicitNamingStrategy != null) {
				hibernateProperties.put("hibernate.implicit_naming_strategy", this.implicitNamingStrategy);
			}
		}

	}

	/**
     * HibernateRuntimeHints class.
     */
    static class HibernateRuntimeHints implements RuntimeHintsRegistrar {

		private static final Consumer<Builder> INVOKE_DECLARED_CONSTRUCTORS = TypeHint
			.builtWith(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);

		/**
         * Registers the hints for the given runtime hints and class loader.
         * 
         * @param hints the runtime hints to register
         * @param classLoader the class loader to use for reflection
         */
        @Override
		public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
			for (String noJtaPlatformClass : NO_JTA_PLATFORM_CLASSES) {
				hints.reflection().registerType(TypeReference.of(noJtaPlatformClass), INVOKE_DECLARED_CONSTRUCTORS);
			}
			hints.reflection().registerType(SpringImplicitNamingStrategy.class, INVOKE_DECLARED_CONSTRUCTORS);
			hints.reflection().registerType(CamelCaseToUnderscoresNamingStrategy.class, INVOKE_DECLARED_CONSTRUCTORS);
		}

	}

}
