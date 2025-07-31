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

package org.springframework.boot.hibernate.autoconfigure;

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
import org.hibernate.boot.model.naming.ImplicitNamingStrategy;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.boot.model.naming.PhysicalNamingStrategySnakeCaseImpl;
import org.hibernate.cfg.ManagedBeanSettings;
import org.jspecify.annotations.Nullable;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeHint;
import org.springframework.aot.hint.TypeHint.Builder;
import org.springframework.aot.hint.TypeReference;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.hibernate.SpringImplicitNamingStrategy;
import org.springframework.boot.hibernate.SpringJtaPlatform;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaConfiguration.HibernateRuntimeHints;
import org.springframework.boot.jdbc.SchemaManagementProvider;
import org.springframework.boot.jdbc.metadata.CompositeDataSourcePoolMetadataProvider;
import org.springframework.boot.jdbc.metadata.DataSourcePoolMetadata;
import org.springframework.boot.jdbc.metadata.DataSourcePoolMetadataProvider;
import org.springframework.boot.jpa.autoconfigure.JpaBaseConfiguration;
import org.springframework.boot.jpa.autoconfigure.JpaProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.jdbc.support.SQLExceptionTranslator;
import org.springframework.jndi.JndiLocatorDelegate;
import org.springframework.orm.jpa.hibernate.SpringBeanContainer;
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

	private final ObjectProvider<SQLExceptionTranslator> sqlExceptionTranslator;

	private final List<HibernatePropertiesCustomizer> hibernatePropertiesCustomizers;

	HibernateJpaConfiguration(DataSource dataSource, JpaProperties jpaProperties,
			ConfigurableListableBeanFactory beanFactory, ObjectProvider<JtaTransactionManager> jtaTransactionManager,
			HibernateProperties hibernateProperties,
			ObjectProvider<Collection<DataSourcePoolMetadataProvider>> metadataProviders,
			ObjectProvider<SchemaManagementProvider> providers,
			ObjectProvider<PhysicalNamingStrategy> physicalNamingStrategy,
			ObjectProvider<ImplicitNamingStrategy> implicitNamingStrategy,
			ObjectProvider<SQLExceptionTranslator> sqlExceptionTranslator,
			ObjectProvider<HibernatePropertiesCustomizer> hibernatePropertiesCustomizers) {
		super(dataSource, jpaProperties, jtaTransactionManager);
		this.hibernateProperties = hibernateProperties;
		this.defaultDdlAutoProvider = new HibernateDefaultDdlAutoProvider(providers);
		this.poolMetadataProvider = new CompositeDataSourcePoolMetadataProvider(metadataProviders.getIfAvailable());
		this.sqlExceptionTranslator = sqlExceptionTranslator;
		this.hibernatePropertiesCustomizers = determineHibernatePropertiesCustomizers(
				physicalNamingStrategy.getIfAvailable(), implicitNamingStrategy.getIfAvailable(), beanFactory,
				hibernatePropertiesCustomizers.orderedStream().toList());
	}

	private List<HibernatePropertiesCustomizer> determineHibernatePropertiesCustomizers(
			@Nullable PhysicalNamingStrategy physicalNamingStrategy,
			@Nullable ImplicitNamingStrategy implicitNamingStrategy, ConfigurableListableBeanFactory beanFactory,
			List<HibernatePropertiesCustomizer> hibernatePropertiesCustomizers) {
		List<HibernatePropertiesCustomizer> customizers = new ArrayList<>();
		if (ClassUtils.isPresent("org.hibernate.resource.beans.container.spi.BeanContainer",
				getClass().getClassLoader())) {
			customizers.add((properties) -> properties.put(ManagedBeanSettings.BEAN_CONTAINER,
					new SpringBeanContainer(beanFactory)));
		}
		if (physicalNamingStrategy != null || implicitNamingStrategy != null) {
			customizers
				.add(new NamingStrategiesHibernatePropertiesCustomizer(physicalNamingStrategy, implicitNamingStrategy));
		}
		customizers.addAll(hibernatePropertiesCustomizers);
		return customizers;
	}

	@Override
	protected AbstractJpaVendorAdapter createJpaVendorAdapter() {
		HibernateJpaVendorAdapter adapter = new HibernateJpaVendorAdapter();
		this.sqlExceptionTranslator.ifUnique(adapter.getJpaDialect()::setJdbcExceptionTranslator);
		return adapter;
	}

	@Override
	protected Map<String, Object> getVendorProperties(DataSource dataSource) {
		Supplier<String> defaultDdlMode = () -> this.defaultDdlAutoProvider.getDefaultDdlAuto(dataSource);
		return new LinkedHashMap<>(this.hibernateProperties.determineHibernateProperties(
				getProperties().getProperties(), new HibernateSettings().ddlAuto(defaultDdlMode)
					.hibernatePropertiesCustomizers(this.hibernatePropertiesCustomizers)));
	}

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

	private void configureProviderDisablesAutocommit(Map<String, Object> vendorProperties) {
		if (isDataSourceAutoCommitDisabled() && !isJta()) {
			vendorProperties.put(PROVIDER_DISABLES_AUTOCOMMIT, "true");
		}
	}

	private boolean isDataSourceAutoCommitDisabled() {
		DataSourcePoolMetadata poolMetadata = this.poolMetadataProvider.getDataSourcePoolMetadata(getDataSource());
		return poolMetadata != null && Boolean.FALSE.equals(poolMetadata.getDefaultAutoCommit());
	}

	private boolean runningOnWebSphere() {
		return ClassUtils.isPresent("com.ibm.websphere.jtaextensions.ExtendedJTATransaction",
				getClass().getClassLoader());
	}

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

	private boolean isUsingJndi() {
		try {
			return JndiLocatorDelegate.isDefaultJndiEnvironmentAvailable();
		}
		catch (Error ex) {
			return false;
		}
	}

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

	private static class NamingStrategiesHibernatePropertiesCustomizer implements HibernatePropertiesCustomizer {

		private final @Nullable PhysicalNamingStrategy physicalNamingStrategy;

		private final @Nullable ImplicitNamingStrategy implicitNamingStrategy;

		NamingStrategiesHibernatePropertiesCustomizer(@Nullable PhysicalNamingStrategy physicalNamingStrategy,
				@Nullable ImplicitNamingStrategy implicitNamingStrategy) {
			this.physicalNamingStrategy = physicalNamingStrategy;
			this.implicitNamingStrategy = implicitNamingStrategy;
		}

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

	static class HibernateRuntimeHints implements RuntimeHintsRegistrar {

		private static final Consumer<Builder> INVOKE_DECLARED_CONSTRUCTORS = TypeHint
			.builtWith(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);

		@Override
		public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
			for (String noJtaPlatformClass : NO_JTA_PLATFORM_CLASSES) {
				hints.reflection().registerType(TypeReference.of(noJtaPlatformClass), INVOKE_DECLARED_CONSTRUCTORS);
			}
			hints.reflection().registerType(SpringImplicitNamingStrategy.class, INVOKE_DECLARED_CONSTRUCTORS);
			hints.reflection().registerType(PhysicalNamingStrategySnakeCaseImpl.class, INVOKE_DECLARED_CONSTRUCTORS);
		}

	}

}
