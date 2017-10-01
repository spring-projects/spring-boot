/*
 * Copyright 2012-2017 the original author or authors.
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

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionMessage;
import org.springframework.boot.autoconfigure.condition.ConditionMessage.Style;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration.HibernateEntityManagerCondition;
import org.springframework.boot.autoconfigure.transaction.TransactionManagerCustomizers;
import org.springframework.boot.jdbc.SchemaManagementProvider;
import org.springframework.boot.orm.jpa.hibernate.SpringJtaPlatform;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.jndi.JndiLocatorDelegate;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.AbstractJpaVendorAdapter;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.jta.JtaTransactionManager;
import org.springframework.util.ClassUtils;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Hibernate JPA.
 *
 * @author Phillip Webb
 * @author Josh Long
 * @author Manuel Doninger
 * @author Andy Wilkinson
 */
@Configuration
@ConditionalOnClass({ LocalContainerEntityManagerFactoryBean.class, EntityManager.class })
@Conditional(HibernateEntityManagerCondition.class)
@AutoConfigureAfter({ DataSourceAutoConfiguration.class })
public class HibernateJpaAutoConfiguration extends JpaBaseConfiguration {

	private static final Log logger = LogFactory
			.getLog(HibernateJpaAutoConfiguration.class);

	private static final String JTA_PLATFORM = "hibernate.transaction.jta.platform";

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
			"org.hibernate.service.jta.platform.internal.WebSphereExtendedJtaPlatform", };

	private final HibernateDefaultDdlAutoProvider defaultDdlAutoProvider;

	public HibernateJpaAutoConfiguration(DataSource dataSource,
			JpaProperties jpaProperties,
			ObjectProvider<JtaTransactionManager> jtaTransactionManager,
			ObjectProvider<TransactionManagerCustomizers> transactionManagerCustomizers,
			ObjectProvider<List<SchemaManagementProvider>> providers) {
		super(dataSource, jpaProperties, jtaTransactionManager,
				transactionManagerCustomizers);
		this.defaultDdlAutoProvider = new HibernateDefaultDdlAutoProvider(
				providers.getIfAvailable(Collections::emptyList));
	}

	@Override
	protected AbstractJpaVendorAdapter createJpaVendorAdapter() {
		return new HibernateJpaVendorAdapter();
	}

	@Override
	protected Map<String, Object> getVendorProperties() {
		Map<String, Object> vendorProperties = new LinkedHashMap<>();
		String defaultDdlMode = this.defaultDdlAutoProvider
				.getDefaultDdlAuto(getDataSource());
		vendorProperties.putAll(getProperties().getHibernateProperties(defaultDdlMode));
		return vendorProperties;
	}

	@Override
	protected void customizeVendorProperties(Map<String, Object> vendorProperties) {
		super.customizeVendorProperties(vendorProperties);
		if (!vendorProperties.containsKey(JTA_PLATFORM)) {
			configureJtaPlatform(vendorProperties);
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

	private boolean runningOnWebSphere() {
		return ClassUtils.isPresent(
				"com.ibm.websphere.jtaextensions." + "ExtendedJTATransaction",
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
			// containers (e.g. JBoss EAP 6) wraps it in the superclass LinkageError
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

	@Order(Ordered.HIGHEST_PRECEDENCE + 20)
	static class HibernateEntityManagerCondition extends SpringBootCondition {

		private static String[] CLASS_NAMES = {
				"org.hibernate.ejb.HibernateEntityManager",
				"org.hibernate.jpa.HibernateEntityManager" };

		@Override
		public ConditionOutcome getMatchOutcome(ConditionContext context,
				AnnotatedTypeMetadata metadata) {
			ConditionMessage.Builder message = ConditionMessage
					.forCondition("HibernateEntityManager");
			for (String className : CLASS_NAMES) {
				if (ClassUtils.isPresent(className, context.getClassLoader())) {
					return ConditionOutcome
							.match(message.found("class").items(Style.QUOTE, className));
				}
			}
			return ConditionOutcome.noMatch(message.didNotFind("class", "classes")
					.items(Style.QUOTE, Arrays.asList(CLASS_NAMES)));
		}

	}

}
