/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.autoconfigure.jdbc;

import javax.sql.DataSource;

import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.boot.autoconfigure.jdbc.DataSourceInitializerPostProcessor.Registrar;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link DataSource}.
 *
 * @author Dave Syer
 * @author Phillip Webb
 */
@Configuration
@ConditionalOnClass({ DataSource.class, EmbeddedDatabaseType.class })
@EnableConfigurationProperties(DataSourceProperties.class)
@Import(Registrar.class)
public class DataSourceAutoConfiguration {

	/**
	 * Determines if the {@code dataSource} being used by Spring was created from
	 * {@link EmbeddedDataSourceConfiguration}.
	 * @return true if the data source was auto-configured.
	 */
	public static boolean containsAutoConfiguredDataSource(
			ConfigurableListableBeanFactory beanFactory) {
		try {
			BeanDefinition beanDefinition = beanFactory.getBeanDefinition("dataSource");
			return EmbeddedDataSourceConfiguration.class.getName().equals(
					beanDefinition.getFactoryBeanName());
		}
		catch (NoSuchBeanDefinitionException ex) {
			return false;
		}
	}

	@Conditional(DataSourceAutoConfiguration.EmbeddedDataSourceCondition.class)
	@ConditionalOnMissingBean(DataSource.class)
	@Import(EmbeddedDataSourceConfiguration.class)
	protected static class EmbeddedConfiguration {

	}

	@Configuration
	@ConditionalOnMissingBean(DataSourceInitializer.class)
	protected static class DataSourceInitializerConfiguration {

		@Bean
		public DataSourceInitializer dataSourceInitializer() {
			return new DataSourceInitializer();
		}

	}

	@Conditional(DataSourceAutoConfiguration.NonEmbeddedDataSourceCondition.class)
	@ConditionalOnMissingBean(DataSource.class)
	protected static class NonEmbeddedConfiguration {

		@Autowired
		private DataSourceProperties properties;

		@Bean
		@ConfigurationProperties(prefix = DataSourceProperties.PREFIX)
		public DataSource dataSource() {
			DataSourceBuilder factory = DataSourceBuilder
					.create(this.properties.getClassLoader())
					.driverClassName(this.properties.getDriverClassName())
					.url(this.properties.getUrl())
					.username(this.properties.getUsername())
					.password(this.properties.getPassword());
			return factory.build();
		}

	}

	@Configuration
	@Conditional(DataSourceAutoConfiguration.DataSourceAvailableCondition.class)
	protected static class JdbcTemplateConfiguration {

		@Autowired(required = false)
		private DataSource dataSource;

		@Bean
		@ConditionalOnMissingBean(JdbcOperations.class)
		public JdbcTemplate jdbcTemplate() {
			return new JdbcTemplate(this.dataSource);
		}

		@Bean
		@ConditionalOnMissingBean(NamedParameterJdbcOperations.class)
		public NamedParameterJdbcTemplate namedParameterJdbcTemplate() {
			return new NamedParameterJdbcTemplate(this.dataSource);
		}

	}

	/**
	 * {@link Condition} to test is a supported non-embedded {@link DataSource} type is
	 * available.
	 */
	static class NonEmbeddedDataSourceCondition extends SpringBootCondition {

		@Override
		public ConditionOutcome getMatchOutcome(ConditionContext context,
				AnnotatedTypeMetadata metadata) {
			if (getDataSourceClassLoader(context) != null) {
				return ConditionOutcome.match("supported DataSource class found");
			}
			return ConditionOutcome.noMatch("missing supported DataSource");
		}

		/**
		 * Returns the class loader for the {@link DataSource} class. Used to ensure that
		 * the driver class can actually be loaded by the data source.
		 */
		private ClassLoader getDataSourceClassLoader(ConditionContext context) {
			Class<?> dataSourceClass = new DataSourceBuilder(context.getClassLoader())
					.findType();
			return (dataSourceClass == null ? null : dataSourceClass.getClassLoader());
		}
	}

	/**
	 * {@link Condition} to detect when an embedded {@link DataSource} type can be used.
	 */
	static class EmbeddedDataSourceCondition extends SpringBootCondition {

		private final SpringBootCondition nonEmbedded = new NonEmbeddedDataSourceCondition();

		@Override
		public ConditionOutcome getMatchOutcome(ConditionContext context,
				AnnotatedTypeMetadata metadata) {
			if (anyMatches(context, metadata, this.nonEmbedded)) {
				return ConditionOutcome
						.noMatch("existing non-embedded database detected");
			}
			EmbeddedDatabaseType type = EmbeddedDatabaseConnection.get(
					context.getClassLoader()).getType();
			if (type == null) {
				return ConditionOutcome.noMatch("no embedded database detected");
			}
			return ConditionOutcome.match("embedded database " + type + " detected");
		}

	}

	/**
	 * {@link Condition} to detect when a {@link DataSource} is available (either because
	 * the user provided one or because one will be auto-configured)
	 */
	static class DataSourceAvailableCondition extends SpringBootCondition {

		private final SpringBootCondition nonEmbedded = new NonEmbeddedDataSourceCondition();

		private final SpringBootCondition embeddedCondition = new EmbeddedDataSourceCondition();

		@Override
		public ConditionOutcome getMatchOutcome(ConditionContext context,
				AnnotatedTypeMetadata metadata) {
			if (hasBean(context, DataSource.class)) {
				return ConditionOutcome
						.match("existing bean configured database detected");
			}
			if (anyMatches(context, metadata, this.nonEmbedded, this.embeddedCondition)) {
				return ConditionOutcome.match("existing auto database detected");
			}
			return ConditionOutcome.noMatch("no existing bean configured database");
		}

		private boolean hasBean(ConditionContext context, Class<?> type) {
			return BeanFactoryUtils.beanNamesForTypeIncludingAncestors(
					context.getBeanFactory(), type, true, false).length > 0;
		}
	}

}
