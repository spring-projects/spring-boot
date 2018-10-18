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

package org.springframework.boot.test.autoconfigure.jdbc;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
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
@Configuration
@AutoConfigureBefore(DataSourceAutoConfiguration.class)
public class TestDatabaseAutoConfiguration {

	private final Environment environment;

	TestDatabaseAutoConfiguration(Environment environment) {
		this.environment = environment;
	}

	@Bean
	@ConditionalOnProperty(prefix = "spring.test.database", name = "replace", havingValue = "AUTO_CONFIGURED")
	@ConditionalOnMissingBean
	public DataSource dataSource() {
		return new EmbeddedDataSourceFactory(this.environment).getEmbeddedDatabase();
	}

	@Bean
	@ConditionalOnProperty(prefix = "spring.test.database", name = "replace", havingValue = "ANY", matchIfMissing = true)
	public static EmbeddedDataSourceBeanFactoryPostProcessor embeddedDataSourceBeanFactoryPostProcessor() {
		return new EmbeddedDataSourceBeanFactoryPostProcessor();
	}

	@Order(Ordered.LOWEST_PRECEDENCE)
	private static class EmbeddedDataSourceBeanFactoryPostProcessor
			implements BeanDefinitionRegistryPostProcessor {

		private static final Log logger = LogFactory
				.getLog(EmbeddedDataSourceBeanFactoryPostProcessor.class);

		@Override
		public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry)
				throws BeansException {
			Assert.isInstanceOf(ConfigurableListableBeanFactory.class, registry,
					"Test Database Auto-configuration can only be "
							+ "used with a ConfigurableListableBeanFactory");
			process(registry, (ConfigurableListableBeanFactory) registry);
		}

		@Override
		public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory)
				throws BeansException {
		}

		private void process(BeanDefinitionRegistry registry,
				ConfigurableListableBeanFactory beanFactory) {
			BeanDefinitionHolder holder = getDataSourceBeanDefinition(beanFactory);
			if (holder != null) {
				String beanName = holder.getBeanName();
				boolean primary = holder.getBeanDefinition().isPrimary();
				logger.info("Replacing '" + beanName + "' DataSource bean with "
						+ (primary ? "primary " : "") + "embedded version");
				registry.removeBeanDefinition(beanName);
				registry.registerBeanDefinition(beanName,
						createEmbeddedBeanDefinition(primary));
			}
		}

		private BeanDefinition createEmbeddedBeanDefinition(boolean primary) {
			BeanDefinition beanDefinition = new RootBeanDefinition(
					EmbeddedDataSourceFactoryBean.class);
			beanDefinition.setPrimary(primary);
			return beanDefinition;
		}

		private BeanDefinitionHolder getDataSourceBeanDefinition(
				ConfigurableListableBeanFactory beanFactory) {
			String[] beanNames = beanFactory.getBeanNamesForType(DataSource.class);
			if (ObjectUtils.isEmpty(beanNames)) {
				logger.warn("No DataSource beans found, "
						+ "embedded version will not be used");
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
			logger.warn("No primary DataSource found, "
					+ "embedded version will not be used");
			return null;
		}

	}

	private static class EmbeddedDataSourceFactoryBean
			implements FactoryBean<DataSource>, EnvironmentAware, InitializingBean {

		private EmbeddedDataSourceFactory factory;

		private EmbeddedDatabase embeddedDatabase;

		@Override
		public void setEnvironment(Environment environment) {
			this.factory = new EmbeddedDataSourceFactory(environment);
		}

		@Override
		public void afterPropertiesSet() throws Exception {
			this.embeddedDatabase = this.factory.getEmbeddedDatabase();
		}

		@Override
		public DataSource getObject() throws Exception {
			return this.embeddedDatabase;
		}

		@Override
		public Class<?> getObjectType() {
			return EmbeddedDatabase.class;
		}

		@Override
		public boolean isSingleton() {
			return true;
		}

	}

	private static class EmbeddedDataSourceFactory {

		private final Environment environment;

		EmbeddedDataSourceFactory(Environment environment) {
			this.environment = environment;
		}

		public EmbeddedDatabase getEmbeddedDatabase() {
			EmbeddedDatabaseConnection connection = this.environment.getProperty(
					"spring.test.database.connection", EmbeddedDatabaseConnection.class,
					EmbeddedDatabaseConnection.NONE);
			if (EmbeddedDatabaseConnection.NONE.equals(connection)) {
				connection = EmbeddedDatabaseConnection.get(getClass().getClassLoader());
			}
			Assert.state(connection != EmbeddedDatabaseConnection.NONE,
					"Failed to replace DataSource with an embedded database for tests. If "
							+ "you want an embedded database please put a supported one "
							+ "on the classpath or tune the replace attribute of "
							+ "@AutoConfigureTestDatabase.");
			return new EmbeddedDatabaseBuilder().generateUniqueName(true)
					.setType(connection.getType()).build();
		}

	}

}
