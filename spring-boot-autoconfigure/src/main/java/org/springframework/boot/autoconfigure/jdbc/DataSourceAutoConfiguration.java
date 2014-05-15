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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.Resource;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.jdbc.datasource.init.DatabasePopulatorUtils;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.util.StringUtils;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link DataSource}.
 * 
 * @author Dave Syer
 * @author Phillip Webb
 */
@Configuration
@ConditionalOnClass(EmbeddedDatabaseType.class)
@EnableConfigurationProperties(DataSourceProperties.class)
public class DataSourceAutoConfiguration {

	private static Log logger = LogFactory.getLog(DataSourceAutoConfiguration.class);

	public static final String CONFIGURATION_PREFIX = "spring.datasource";

	@Autowired(required = false)
	private DataSource dataSource;

	@Autowired
	private ApplicationContext applicationContext;

	@Autowired
	private DataSourceProperties properties;

	@PostConstruct
	protected void initialize() throws Exception {
		boolean initialize = this.properties.isInitialize();
		if (this.dataSource == null || !initialize) {
			logger.debug("No DataSource found so not initializing");
			return;
		}

		String schema = this.properties.getSchema();
		if (schema == null) {
			String platform = this.properties.getPlatform();
			schema = "classpath*:schema-" + platform + ".sql,";
			schema += "classpath*:schema.sql,";
			schema += "classpath*:data-" + platform + ".sql,";
			schema += "classpath*:data.sql";
		}

		List<Resource> resources = new ArrayList<Resource>();
		for (String schemaLocation : StringUtils.commaDelimitedListToStringArray(schema)) {
			resources.addAll(Arrays.asList(this.applicationContext
					.getResources(schemaLocation)));
		}

		boolean continueOnError = this.properties.isContinueOnError();
		boolean exists = false;
		ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
		for (Resource resource : resources) {
			if (resource.exists()) {
				exists = true;
				populator.addScript(resource);
				populator.setContinueOnError(continueOnError);
			}
		}
		populator.setSeparator(this.properties.getSeparator());

		if (exists) {
			DatabasePopulatorUtils.execute(populator, this.dataSource);
		}
	}

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

	@Conditional(DataSourceAutoConfiguration.EmbeddedDatabaseCondition.class)
	@ConditionalOnMissingBean(DataSource.class)
	@Import(EmbeddedDataSourceConfiguration.class)
	protected static class EmbeddedConfiguration {

	}

	@Conditional(DataSourceAutoConfiguration.NonEmbeddedDatabaseCondition.class)
	@ConditionalOnMissingBean(DataSource.class)
	protected static class NonEmbeddedConfiguration {

		@Autowired
		private DataSourceProperties properties;

		@ConfigurationProperties(prefix = DataSourceAutoConfiguration.CONFIGURATION_PREFIX)
		@Bean
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
	@Conditional(DataSourceAutoConfiguration.DatabaseCondition.class)
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
	 * Base {@link Condition} for non-embedded database checks.
	 */
	static class NonEmbeddedDatabaseCondition extends SpringBootCondition {

		@Override
		public ConditionOutcome getMatchOutcome(ConditionContext context,
				AnnotatedTypeMetadata metadata) {

			ClassLoader dataSourceClassLoader = getDataSourceClassLoader(context);
			if (dataSourceClassLoader != null) {
				return ConditionOutcome.match("Supported DataSource class found");
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
			if (dataSourceClass == null) {
				return null;
			}
			return dataSourceClass.getClassLoader();
		}

	}

	/**
	 * {@link Condition} to detect when an embedded database is used.
	 */
	static class EmbeddedDatabaseCondition extends SpringBootCondition {

		private final SpringBootCondition nonEmbedded = new NonEmbeddedDatabaseCondition();

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
	 * {@link Condition} to detect when a database is configured.
	 */
	static class DatabaseCondition extends SpringBootCondition {

		private final SpringBootCondition nonEmbedded = new NonEmbeddedDatabaseCondition();

		private final SpringBootCondition embeddedCondition = new EmbeddedDatabaseCondition();

		@Override
		public ConditionOutcome getMatchOutcome(ConditionContext context,
				AnnotatedTypeMetadata metadata) {

			if (anyMatches(context, metadata, this.nonEmbedded, this.embeddedCondition)) {
				return ConditionOutcome.match("existing auto database detected");
			}

			if (BeanFactoryUtils.beanNamesForTypeIncludingAncestors(
					context.getBeanFactory(), DataSource.class, true, false).length > 0) {
				return ConditionOutcome
						.match("Existing bean configured database detected");
			}

			return ConditionOutcome.noMatch("no existing bean configured database");
		}

	}

}
