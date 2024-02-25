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

package org.springframework.boot.test.autoconfigure.jdbc;

import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aot.AotDetector;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Role;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * Auto-configuration for a test database.
 *
 * @author Phillip Webb
 * @since 1.4.0
 * @see AutoConfigureTestDatabase
 */
@AutoConfiguration(before = DataSourceAutoConfiguration.class)
public class TestDatabaseAutoConfiguration {

	/**
	 * Creates a DataSource bean if the property "spring.test.database.replace" is set to
	 * "AUTO_CONFIGURED" and no other bean of type DataSource is present.
	 * @param environment the environment object used to retrieve the property value
	 * @return a DataSource bean representing the embedded database
	 */
	@Bean
	@ConditionalOnProperty(prefix = "spring.test.database", name = "replace", havingValue = "AUTO_CONFIGURED")
	@ConditionalOnMissingBean
	public DataSource dataSource(Environment environment) {
		return new EmbeddedDataSourceFactory(environment).getEmbeddedDatabase();
	}

	/**
	 * Creates a bean factory post processor for configuring an embedded data source. This
	 * bean factory post processor is only enabled if the property
	 * "spring.test.database.replace" is set to "ANY", or if the property is missing.
	 * @return the embedded data source bean factory post processor
	 */
	@Bean
	@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
	@ConditionalOnProperty(prefix = "spring.test.database", name = "replace", havingValue = "ANY",
			matchIfMissing = true)
	static EmbeddedDataSourceBeanFactoryPostProcessor embeddedDataSourceBeanFactoryPostProcessor() {
		return new EmbeddedDataSourceBeanFactoryPostProcessor();
	}

	/**
	 * EmbeddedDataSourceBeanFactoryPostProcessor class.
	 */
	@Order(Ordered.LOWEST_PRECEDENCE)
	static class EmbeddedDataSourceBeanFactoryPostProcessor implements BeanDefinitionRegistryPostProcessor {

		private static final Log logger = LogFactory.getLog(EmbeddedDataSourceBeanFactoryPostProcessor.class);

		/**
		 * This method is called during the post-processing phase of the bean definition
		 * registry. It checks if the AotDetector is using generated artifacts and returns
		 * if true. Otherwise, it asserts that the registry is an instance of
		 * ConfigurableListableBeanFactory. Then, it calls the process method passing the
		 * registry and the ConfigurableListableBeanFactory.
		 * @param registry The bean definition registry to be processed.
		 * @throws BeansException if any error occurs during the processing.
		 * @see AotDetector
		 * @see ConfigurableListableBeanFactory
		 * @see #process(BeanDefinitionRegistry, ConfigurableListableBeanFactory)
		 */
		@Override
		public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
			if (AotDetector.useGeneratedArtifacts()) {
				return;
			}
			Assert.isInstanceOf(ConfigurableListableBeanFactory.class, registry,
					"Test Database Auto-configuration can only be used with a ConfigurableListableBeanFactory");
			process(registry, (ConfigurableListableBeanFactory) registry);
		}

		/**
		 * Post-process the given bean factory.
		 * @param beanFactory the bean factory to post-process
		 * @throws BeansException if any error occurs during post-processing
		 */
		@Override
		public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		}

		/**
		 * Replaces the DataSource bean with an embedded version if it exists.
		 * @param registry the BeanDefinitionRegistry to modify
		 * @param beanFactory the ConfigurableListableBeanFactory to retrieve the
		 * DataSource bean from
		 */
		private void process(BeanDefinitionRegistry registry, ConfigurableListableBeanFactory beanFactory) {
			BeanDefinitionHolder holder = getDataSourceBeanDefinition(beanFactory);
			if (holder != null) {
				String beanName = holder.getBeanName();
				boolean primary = holder.getBeanDefinition().isPrimary();
				logger.info("Replacing '" + beanName + "' DataSource bean with " + (primary ? "primary " : "")
						+ "embedded version");
				registry.removeBeanDefinition(beanName);
				registry.registerBeanDefinition(beanName, createEmbeddedBeanDefinition(primary));
			}
		}

		/**
		 * Creates a new embedded bean definition for the embedded data source factory
		 * bean.
		 * @param primary true if the bean should be marked as primary, false otherwise
		 * @return the created bean definition
		 */
		private BeanDefinition createEmbeddedBeanDefinition(boolean primary) {
			BeanDefinition beanDefinition = new RootBeanDefinition(EmbeddedDataSourceFactoryBean.class);
			beanDefinition.setPrimary(primary);
			return beanDefinition;
		}

		/**
		 * Retrieves the bean definition for the primary DataSource bean from the given
		 * bean factory. If no DataSource beans are found, a warning message is logged and
		 * null is returned. If only one DataSource bean is found, its bean definition is
		 * returned. If multiple DataSource beans are found, the bean definition for the
		 * primary DataSource bean is returned. If no primary DataSource bean is found, a
		 * warning message is logged and null is returned.
		 * @param beanFactory the bean factory to retrieve the DataSource bean definition
		 * from
		 * @return the bean definition holder for the primary DataSource bean, or null if
		 * no DataSource beans are found or no primary DataSource bean is found
		 */
		private BeanDefinitionHolder getDataSourceBeanDefinition(ConfigurableListableBeanFactory beanFactory) {
			String[] beanNames = beanFactory.getBeanNamesForType(DataSource.class);
			if (ObjectUtils.isEmpty(beanNames)) {
				logger.warn("No DataSource beans found, embedded version will not be used");
				return null;
			}
			if (beanNames.length == 1) {
				String beanName = beanNames[0];
				BeanDefinition beanDefinition = beanFactory.getBeanDefinition(beanName);
				return new BeanDefinitionHolder(beanDefinition, beanName);
			}
			for (String beanName : beanNames) {
				BeanDefinition beanDefinition = beanFactory.getBeanDefinition(beanName);
				if (beanDefinition.isPrimary()) {
					return new BeanDefinitionHolder(beanDefinition, beanName);
				}
			}
			logger.warn("No primary DataSource found, embedded version will not be used");
			return null;
		}

	}

	/**
	 * EmbeddedDataSourceFactoryBean class.
	 */
	static class EmbeddedDataSourceFactoryBean implements FactoryBean<DataSource>, EnvironmentAware, InitializingBean {

		private EmbeddedDataSourceFactory factory;

		private EmbeddedDatabase embeddedDatabase;

		/**
		 * Sets the environment for the EmbeddedDataSourceFactory.
		 * @param environment the environment to set
		 */
		@Override
		public void setEnvironment(Environment environment) {
			this.factory = new EmbeddedDataSourceFactory(environment);
		}

		/**
		 * Initializes the embedded database by creating an instance of the embedded
		 * database using the factory.
		 * @throws Exception if an error occurs during the initialization process
		 */
		@Override
		public void afterPropertiesSet() throws Exception {
			this.embeddedDatabase = this.factory.getEmbeddedDatabase();
		}

		/**
		 * Returns the embedded database object.
		 * @return the embedded database object
		 * @throws Exception if an error occurs while retrieving the embedded database
		 * object
		 */
		@Override
		public DataSource getObject() throws Exception {
			return this.embeddedDatabase;
		}

		/**
		 * Returns the type of object that is created by this factory bean.
		 * @return the type of object created by this factory bean, which is
		 * EmbeddedDatabase.
		 */
		@Override
		public Class<?> getObjectType() {
			return EmbeddedDatabase.class;
		}

	}

	/**
	 * EmbeddedDataSourceFactory class.
	 */
	static class EmbeddedDataSourceFactory {

		private final Environment environment;

		/**
		 * Constructs a new EmbeddedDataSourceFactory object with the given environment.
		 * @param environment the environment to be used for configuring the data source
		 */
		EmbeddedDataSourceFactory(Environment environment) {
			this.environment = environment;
			if (environment instanceof ConfigurableEnvironment configurableEnvironment) {
				Map<String, Object> source = new HashMap<>();
				source.put("spring.datasource.schema-username", "");
				source.put("spring.sql.init.username", "");
				configurableEnvironment.getPropertySources().addFirst(new MapPropertySource("testDatabase", source));
			}
		}

		/**
		 * Retrieves an embedded database based on the specified connection type. If no
		 * connection type is specified, it will try to determine the connection type
		 * based on the class loader. If no supported embedded database is found, an
		 * exception will be thrown.
		 * @return the embedded database
		 * @throws IllegalStateException if no supported embedded database is found
		 */
		EmbeddedDatabase getEmbeddedDatabase() {
			EmbeddedDatabaseConnection connection = this.environment.getProperty("spring.test.database.connection",
					EmbeddedDatabaseConnection.class, EmbeddedDatabaseConnection.NONE);
			if (EmbeddedDatabaseConnection.NONE.equals(connection)) {
				connection = EmbeddedDatabaseConnection.get(getClass().getClassLoader());
			}
			Assert.state(connection != EmbeddedDatabaseConnection.NONE,
					"Failed to replace DataSource with an embedded database for tests. If "
							+ "you want an embedded database please put a supported one "
							+ "on the classpath or tune the replace attribute of @AutoConfigureTestDatabase.");
			return new EmbeddedDatabaseBuilder().generateUniqueName(true).setType(connection.getType()).build();
		}

	}

}
