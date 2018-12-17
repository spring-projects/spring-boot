/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.autoconfigure.data.neo4j;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.neo4j.ogm.driver.NativeTypesException;
import org.neo4j.ogm.driver.NativeTypesNotAvailableException;
import org.neo4j.ogm.driver.NativeTypesNotSupportedException;
import org.neo4j.ogm.session.SessionFactory;
import org.neo4j.ogm.session.event.EventListener;

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
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.source.InvalidConfigurationPropertyValueException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.neo4j.transaction.Neo4jTransactionManager;
import org.springframework.data.neo4j.web.support.OpenSessionInViewInterceptor;
import org.springframework.lang.Nullable;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Spring Data Neo4j.
 *
 * @author Michael Hunger
 * @author Josh Long
 * @author Vince Bickers
 * @author Stephane Nicoll
 * @author Kazuki Shimizu
 * @author Michael Simons
 * @since 1.4.0
 */
@Configuration
@ConditionalOnClass({ SessionFactory.class, Neo4jTransactionManager.class,
		PlatformTransactionManager.class })
@ConditionalOnMissingBean(SessionFactory.class)
@EnableConfigurationProperties(Neo4jProperties.class)
@Import(Neo4jBookmarkManagementConfiguration.class)
public class Neo4jDataAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public org.neo4j.ogm.config.Configuration configuration(Neo4jProperties properties) {
		return properties.createConfiguration();
	}

	@Bean
	public SessionFactory sessionFactory(org.neo4j.ogm.config.Configuration configuration,
			ApplicationContext applicationContext,
			ObjectProvider<EventListener> eventListeners) {
		try {
			SessionFactory sessionFactory = new SessionFactory(configuration,
					getPackagesToScan(applicationContext));
			eventListeners.forEach(sessionFactory::register);
			return sessionFactory;
		}
		catch (NativeTypesException ex) {
			InvalidConfigurationPropertyValueException translatedMessage = translateNativeTypesException(
					ex);
			throw (translatedMessage != null) ? translatedMessage : ex;
		}
	}

	@Nullable
	private static InvalidConfigurationPropertyValueException translateNativeTypesException(
			NativeTypesException cause) {

		String propertyName = Neo4jProperties.CONFIGURATION_PREFIX + ".use-native-types";
		boolean propertyValue = true;

		if (cause instanceof NativeTypesNotAvailableException) {
			String message = String.format(
					"The native type module for your Neo4j-OGM driver is not available. "
							+ "Please add the following dependency to your build:%n'%s'.",
					((NativeTypesNotAvailableException) cause).getRequiredModule());
			return new InvalidConfigurationPropertyValueException(propertyName,
					propertyValue, message);
		}
		if (cause instanceof NativeTypesNotSupportedException) {
			String message = String.format(
					"The configured Neo4j-OGM driver %s does not support Neo4j native types. "
							+ "Please consider one of the drivers that support Neo4js native types like the Bolt or the embedded driver.",
					cause.getDriverClassName());
			return new InvalidConfigurationPropertyValueException(propertyName,
					propertyValue, message);
		}

		return null;
	}

	@Bean
	@ConditionalOnMissingBean(PlatformTransactionManager.class)
	public Neo4jTransactionManager transactionManager(SessionFactory sessionFactory,
			Neo4jProperties properties,
			ObjectProvider<TransactionManagerCustomizers> transactionManagerCustomizers) {
		return customize(new Neo4jTransactionManager(sessionFactory),
				transactionManagerCustomizers.getIfAvailable());
	}

	private Neo4jTransactionManager customize(Neo4jTransactionManager transactionManager,
			TransactionManagerCustomizers customizers) {
		if (customizers != null) {
			customizers.customize(transactionManager);
		}
		return transactionManager;
	}

	private String[] getPackagesToScan(ApplicationContext applicationContext) {
		List<String> packages = EntityScanPackages.get(applicationContext)
				.getPackageNames();
		if (packages.isEmpty() && AutoConfigurationPackages.has(applicationContext)) {
			packages = AutoConfigurationPackages.get(applicationContext);
		}
		return StringUtils.toStringArray(packages);
	}

	@Configuration
	@ConditionalOnWebApplication(type = Type.SERVLET)
	@ConditionalOnClass({ WebMvcConfigurer.class, OpenSessionInViewInterceptor.class })
	@ConditionalOnMissingBean(OpenSessionInViewInterceptor.class)
	@ConditionalOnProperty(prefix = "spring.data.neo4j", name = "open-in-view", havingValue = "true", matchIfMissing = true)
	protected static class Neo4jWebConfiguration {

		@Configuration
		protected static class Neo4jWebMvcConfiguration implements WebMvcConfigurer {

			private static final Log logger = LogFactory
					.getLog(Neo4jWebMvcConfiguration.class);

			private final Neo4jProperties neo4jProperties;

			protected Neo4jWebMvcConfiguration(Neo4jProperties neo4jProperties) {
				this.neo4jProperties = neo4jProperties;
			}

			@Bean
			public OpenSessionInViewInterceptor neo4jOpenSessionInViewInterceptor() {
				if (this.neo4jProperties.getOpenInView() == null) {
					logger.warn("spring.data.neo4j.open-in-view is enabled by default."
							+ "Therefore, database queries may be performed during view "
							+ "rendering. Explicitly configure "
							+ "spring.data.neo4j.open-in-view to disable this warning");
				}
				return new OpenSessionInViewInterceptor();
			}

			@Override
			public void addInterceptors(InterceptorRegistry registry) {
				registry.addWebRequestInterceptor(neo4jOpenSessionInViewInterceptor());
			}

		}

	}

}
