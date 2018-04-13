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

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.boot.model.naming.ImplicitNamingStrategy;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.boot.autoconfigure.transaction.TransactionManagerCustomizers;
import org.springframework.boot.jdbc.SchemaManagementProvider;
import org.springframework.boot.jdbc.metadata.CompositeDataSourcePoolMetadataProvider;
import org.springframework.boot.jdbc.metadata.DataSourcePoolMetadata;
import org.springframework.boot.jdbc.metadata.DataSourcePoolMetadataProvider;
import org.springframework.boot.orm.jpa.hibernate.SpringJtaPlatform;
import org.springframework.context.annotation.Configuration;
import org.springframework.jndi.JndiLocatorDelegate;
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
 * @since 2.0.0
 */
@Configuration
@ConditionalOnSingleCandidate(DataSource.class)
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

	/**
	 * {@code WebSphereExtendedJtaPlatform} implementations for various Hibernate
	 * versions.
	 */
	private static final String[] WEBSPHERE_JTA_PLATFORM_CLASSES = {
			"org.hibernate.engine.transaction.jta.platform.internal.WebSphereExtendedJtaPlatform",
			"org.hibernate.service.jta.platform.internal.WebSphereExtendedJtaPlatform" };

	private final HibernateDefaultDdlAutoProvider defaultDdlAutoProvider;

	private DataSourcePoolMetadataProvider poolMetadataProvider;

	private final PhysicalNamingStrategy physicalNamingStrategy;

	private final ImplicitNamingStrategy implicitNamingStrategy;

	private final List<HibernatePropertiesCustomizer> hibernatePropertiesCustomizers;

	HibernateJpaConfiguration(DataSource dataSource, JpaProperties jpaProperties,
			ObjectProvider<JtaTransactionManager> jtaTransactionManager,
			ObjectProvider<TransactionManagerCustomizers> transactionManagerCustomizers,
			ObjectProvider<Collection<DataSourcePoolMetadataProvider>> metadataProviders,
			ObjectProvider<List<SchemaManagementProvider>> providers,
			ObjectProvider<PhysicalNamingStrategy> physicalNamingStrategy,
			ObjectProvider<ImplicitNamingStrategy> implicitNamingStrategy,
			ObjectProvider<List<HibernatePropertiesCustomizer>> hibernatePropertiesCustomizers) {
		super(dataSource, jpaProperties, jtaTransactionManager,
				transactionManagerCustomizers);
		this.defaultDdlAutoProvider = new HibernateDefaultDdlAutoProvider(
				providers.getIfAvailable(Collections::emptyList));
		this.poolMetadataProvider = new CompositeDataSourcePoolMetadataProvider(
				metadataProviders.getIfAvailable());
		this.physicalNamingStrategy = physicalNamingStrategy.getIfAvailable();
		this.implicitNamingStrategy = implicitNamingStrategy.getIfAvailable();
		this.hibernatePropertiesCustomizers = hibernatePropertiesCustomizers
				.getIfAvailable(() -> Collections.emptyList());
	}

	@Override
	protected AbstractJpaVendorAdapter createJpaVendorAdapter() {
		return new HibernateJpaVendorAdapter();
	}

	@Override
	protected Map<String, Object> getVendorProperties() {
		Supplier<String> defaultDdlMode = () -> this.defaultDdlAutoProvider
				.getDefaultDdlAuto(getDataSource());
		return new LinkedHashMap<>(getProperties()
				.getHibernateProperties(new HibernateSettings().ddlAuto(defaultDdlMode)
						.implicitNamingStrategy(this.implicitNamingStrategy)
						.physicalNamingStrategy(this.physicalNamingStrategy)
						.hibernatePropertiesCustomizers(
								this.hibernatePropertiesCustomizers)));
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

	private void configureJtaPlatform(Map<String, Object> vendorProperties)
			throws LinkageError {
		JtaTransactionManager jtaTransactionManager = getJtaTransactionManager();
		if (jtaTransactionManager != null) {
			if (runningOnWebSphere()) {
				// We can never use SpringJtaPlatform on WebSphere as
				// WebSphereUowTransactionManager has a null TransactionManager
				// which will cause Hibernate to NPE
				configureWebSphereTransactionPlatform(vendorProperties);
			}
			else {
				configureSpringJtaPlatform(vendorProperties, jtaTransactionManager);
			}
		}
		else {
			vendorProperties.put(JTA_PLATFORM, getNoJtaPlatformManager());
		}
	}

	private void configureProviderDisablesAutocommit(
			Map<String, Object> vendorProperties) {
		if (isDataSourceAutoCommitDisabled() && !isJta()) {
			vendorProperties.put(PROVIDER_DISABLES_AUTOCOMMIT, "true");
		}
	}

	private boolean isDataSourceAutoCommitDisabled() {
		DataSourcePoolMetadata poolMetadata = this.poolMetadataProvider
				.getDataSourcePoolMetadata(getDataSource());
		return poolMetadata != null
				&& Boolean.FALSE.equals(poolMetadata.getDefaultAutoCommit());
	}

	private boolean runningOnWebSphere() {
		return ClassUtils.isPresent(
				"com.ibm.websphere.jtaextensions.ExtendedJTATransaction",
				getClass().getClassLoader());
	}

	private void configureWebSphereTransactionPlatform(
			Map<String, Object> vendorProperties) {
		vendorProperties.put(JTA_PLATFORM, getWebSphereJtaPlatformManager());
	}

	private Object getWebSphereJtaPlatformManager() {
		return getJtaPlatformManager(WEBSPHERE_JTA_PLATFORM_CLASSES);
	}

	private void configureSpringJtaPlatform(Map<String, Object> vendorProperties,
			JtaTransactionManager jtaTransactionManager) {
		try {
			vendorProperties.put(JTA_PLATFORM,
					new SpringJtaPlatform(jtaTransactionManager));
		}
		catch (LinkageError ex) {
			// NoClassDefFoundError can happen if Hibernate 4.2 is used and some
			// containers (e.g. JBoss EAP 6) wrap it in the superclass LinkageError
			if (!isUsingJndi()) {
				throw new IllegalStateException("Unable to set Hibernate JTA "
						+ "platform, are you using the correct "
						+ "version of Hibernate?", ex);
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
		return getJtaPlatformManager(NO_JTA_PLATFORM_CLASSES);
	}

	private Object getJtaPlatformManager(String[] candidates) {
		for (String candidate : candidates) {
			try {
				return Class.forName(candidate).newInstance();
			}
			catch (Exception ex) {
				// Continue searching
			}
		}
		throw new IllegalStateException("Could not configure JTA platform");
	}

}
