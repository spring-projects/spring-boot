/*
 * Copyright 2012-2025 the original author or authors.
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aot.AotDetector;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.container.ContainerImageMetadata;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcConnectionDetails;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.bind.BoundPropertiesTrackingBindHandler;
import org.springframework.boot.context.properties.source.ConfigurationProperty;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
import org.springframework.boot.origin.PropertySourceOrigin;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Role;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.type.MethodMetadata;
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

	@Bean
	@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
	@ConditionalOnProperty(name = "spring.test.database.replace", havingValue = "NON_TEST", matchIfMissing = true)
	static EmbeddedDataSourceBeanFactoryPostProcessor nonTestEmbeddedDataSourceBeanFactoryPostProcessor(
			Environment environment) {
		return new EmbeddedDataSourceBeanFactoryPostProcessor(environment, Replace.NON_TEST);
	}

	@Bean
	@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
	@ConditionalOnProperty(name = "spring.test.database.replace", havingValue = "ANY")
	static EmbeddedDataSourceBeanFactoryPostProcessor embeddedDataSourceBeanFactoryPostProcessor(
			Environment environment) {
		return new EmbeddedDataSourceBeanFactoryPostProcessor(environment, Replace.ANY);
	}

	@Bean
	@ConditionalOnProperty(name = "spring.test.database.replace", havingValue = "AUTO_CONFIGURED")
	@ConditionalOnMissingBean
	public DataSource dataSource(Environment environment) {
		return new EmbeddedDataSourceFactory(environment).getEmbeddedDatabase();
	}

	@Order(Ordered.LOWEST_PRECEDENCE)
	static class EmbeddedDataSourceBeanFactoryPostProcessor implements BeanDefinitionRegistryPostProcessor {

		private static final ConfigurationPropertyName DATASOURCE_URL_PROPERTY = ConfigurationPropertyName
			.of("spring.datasource.url");

		private static final Bindable<String> BINDABLE_STRING = Bindable.of(String.class);

		private static final String DYNAMIC_VALUES_PROPERTY_SOURCE_CLASS = "org.springframework.test.context.support.DynamicValuesPropertySource";

		private static final Log logger = LogFactory.getLog(EmbeddedDataSourceBeanFactoryPostProcessor.class);

		private final Environment environment;

		private final Replace replace;

		EmbeddedDataSourceBeanFactoryPostProcessor(Environment environment, Replace replace) {
			this.environment = environment;
			this.replace = replace;
		}

		@Override
		public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
			if (AotDetector.useGeneratedArtifacts()) {
				return;
			}
			Assert.isTrue(registry instanceof ConfigurableListableBeanFactory,
					"'registry' must be a ConfigurableListableBeanFactory");
			process(registry, (ConfigurableListableBeanFactory) registry);
		}

		@Override
		public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		}

		private void process(BeanDefinitionRegistry registry, ConfigurableListableBeanFactory beanFactory) {
			BeanDefinitionHolder holder = getDataSourceBeanDefinition(beanFactory);
			if (holder != null && isReplaceable(beanFactory, holder)) {
				String beanName = holder.getBeanName();
				boolean primary = holder.getBeanDefinition().isPrimary();
				logger.info("Replacing '" + beanName + "' DataSource bean with " + (primary ? "primary " : "")
						+ "embedded version");
				registry.removeBeanDefinition(beanName);
				registry.registerBeanDefinition(beanName, createEmbeddedBeanDefinition(primary));
			}
		}

		private BeanDefinition createEmbeddedBeanDefinition(boolean primary) {
			BeanDefinition beanDefinition = new RootBeanDefinition(EmbeddedDataSourceFactoryBean.class);
			beanDefinition.setPrimary(primary);
			return beanDefinition;
		}

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

		private boolean isReplaceable(ConfigurableListableBeanFactory beanFactory, BeanDefinitionHolder holder) {
			if (this.replace == Replace.NON_TEST) {
				return !isAutoConfigured(holder) || !isConnectingToTestDatabase(beanFactory);
			}
			return true;
		}

		private boolean isAutoConfigured(BeanDefinitionHolder holder) {
			if (holder.getBeanDefinition() instanceof AnnotatedBeanDefinition annotatedBeanDefinition) {
				MethodMetadata factoryMethodMetadata = annotatedBeanDefinition.getFactoryMethodMetadata();
				return (factoryMethodMetadata != null) && (factoryMethodMetadata.getDeclaringClassName()
					.startsWith("org.springframework.boot.autoconfigure."));
			}
			return false;
		}

		private boolean isConnectingToTestDatabase(ConfigurableListableBeanFactory beanFactory) {
			return isUsingTestServiceConnection(beanFactory) || isUsingTestDatasourceUrl();
		}

		private boolean isUsingTestServiceConnection(ConfigurableListableBeanFactory beanFactory) {
			for (String beanName : beanFactory.getBeanNamesForType(JdbcConnectionDetails.class)) {
				try {
					BeanDefinition beanDefinition = beanFactory.getBeanDefinition(beanName);
					if (ContainerImageMetadata.isPresent(beanDefinition)) {
						return true;
					}
				}
				catch (NoSuchBeanDefinitionException ex) {
					// Ignore
				}
			}
			return false;
		}

		private boolean isUsingTestDatasourceUrl() {
			List<ConfigurationProperty> bound = new ArrayList<>();
			Binder.get(this.environment, new BoundPropertiesTrackingBindHandler(bound::add))
				.bind(DATASOURCE_URL_PROPERTY, BINDABLE_STRING);
			return !bound.isEmpty() && isUsingTestDatasourceUrl(bound.get(0));
		}

		private boolean isUsingTestDatasourceUrl(ConfigurationProperty configurationProperty) {
			return isBoundToDynamicValuesPropertySource(configurationProperty)
					|| isTestcontainersUrl(configurationProperty);
		}

		private boolean isBoundToDynamicValuesPropertySource(ConfigurationProperty configurationProperty) {
			if (configurationProperty.getOrigin() instanceof PropertySourceOrigin origin) {
				return isDynamicValuesPropertySource(origin.getPropertySource());
			}
			return false;
		}

		private boolean isDynamicValuesPropertySource(PropertySource<?> propertySource) {
			return propertySource != null
					&& DYNAMIC_VALUES_PROPERTY_SOURCE_CLASS.equals(propertySource.getClass().getName());
		}

		private boolean isTestcontainersUrl(ConfigurationProperty configurationProperty) {
			Object value = configurationProperty.getValue();
			return (value != null) && value.toString().startsWith("jdbc:tc:");
		}

	}

	static class EmbeddedDataSourceFactoryBean implements FactoryBean<DataSource>, EnvironmentAware, InitializingBean {

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

	}

	static class EmbeddedDataSourceFactory {

		private final Environment environment;

		EmbeddedDataSourceFactory(Environment environment) {
			this.environment = environment;
			if (environment instanceof ConfigurableEnvironment configurableEnvironment) {
				Map<String, Object> source = new HashMap<>();
				source.put("spring.datasource.schema-username", "");
				source.put("spring.sql.init.username", "");
				configurableEnvironment.getPropertySources().addFirst(new MapPropertySource("testDatabase", source));
			}
		}

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
