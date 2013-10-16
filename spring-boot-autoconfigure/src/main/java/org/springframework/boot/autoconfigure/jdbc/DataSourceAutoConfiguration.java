/*
 * Copyright 2012-2013 the original author or authors.
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
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.boot.bind.RelaxedPropertyResolver;
import org.springframework.context.ApplicationContext;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.jdbc.datasource.init.DatabasePopulatorUtils;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link DataSource}.
 * 
 * @author Dave Syer
 * @author Phillip Webb
 */
@Configuration
@ConditionalOnClass(EmbeddedDatabaseType.class /* Spring JDBC */)
public class DataSourceAutoConfiguration implements EnvironmentAware {

	private static Log logger = LogFactory.getLog(DataSourceAutoConfiguration.class);

	public static final String CONFIGURATION_PREFIX = "spring.datasource";

	@Autowired(required = false)
	private DataSource dataSource;

	@Autowired
	private ApplicationContext applicationContext;

	private RelaxedPropertyResolver environment;

	@Override
	public void setEnvironment(Environment environment) {
		this.environment = new RelaxedPropertyResolver(environment, CONFIGURATION_PREFIX
				+ ".");
	}

	@PostConstruct
	protected void initialize() throws Exception {
		if (this.dataSource == null) {
			logger.debug("No DataSource found so not initializing");
			return;
		}

		String schema = this.environment.getProperty("schema");
		if (schema == null) {
			schema = "classpath*:schema-"
					+ this.environment.getProperty("platform", "all")
					+ ".sql,classpath*:schema.sql,classpath*:data.sql";
		}

		List<Resource> resources = new ArrayList<Resource>();
		for (String schemaLocation : StringUtils.commaDelimitedListToStringArray(schema)) {
			resources.addAll(Arrays.asList(this.applicationContext
					.getResources(schemaLocation)));
		}

		boolean exists = false;
		ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
		for (Resource resource : resources) {
			if (resource.exists()) {
				exists = true;
				populator.addScript(resource);
				populator.setContinueOnError(true);
			}
		}

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

	@Conditional(DataSourceAutoConfiguration.TomcatDatabaseCondition.class)
	@ConditionalOnMissingBean(DataSource.class)
	@Import(TomcatDataSourceConfiguration.class)
	protected static class TomcatConfiguration {
	}

	@Conditional(DataSourceAutoConfiguration.BasicDatabaseCondition.class)
	@ConditionalOnMissingBean(DataSource.class)
	@Import(CommonsDataSourceConfiguration.class)
	protected static class DbcpConfiguration {
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
		public NamedParameterJdbcOperations namedParameterJdbcTemplate() {
			return new NamedParameterJdbcTemplate(this.dataSource);
		}

	}

	static abstract class NonEmbeddedDatabaseCondition extends SpringBootCondition {

		protected abstract String getDataSourceClassName();

		@Override
		public Outcome getMatchOutcome(ConditionContext context,
				AnnotatedTypeMetadata metadata) {

			if (!ClassUtils.isPresent(getDataSourceClassName(), context.getClassLoader())) {
				return Outcome.noMatch(getDataSourceClassName()
						+ " DataSource class not found");
			}

			String driverClassName = getDriverClassName(context.getEnvironment(),
					getDataSourceClassLoader(context));
			if (driverClassName == null) {
				return Outcome.noMatch("no database driver");
			}

			String url = getUrl(context.getEnvironment(), context.getClassLoader());
			if (url == null) {
				return Outcome.noMatch("no database URL");
			}

			if (ClassUtils.isPresent(driverClassName, context.getClassLoader())) {
				return Outcome.match("found database driver " + driverClassName);
			}

			return Outcome.noMatch("missing database driver " + driverClassName);
		}

		/**
		 * Returns the class loader for the {@link DataSource} class. Used to ensure that
		 * the driver class can actually be loaded by the data source.
		 */
		private ClassLoader getDataSourceClassLoader(ConditionContext context) {
			try {
				Class<?> dataSourceClass = ClassUtils.forName(getDataSourceClassName(),
						context.getClassLoader());
				return dataSourceClass.getClassLoader();
			}
			catch (ClassNotFoundException ex) {
				throw new IllegalStateException(ex);
			}
		}

		private String getDriverClassName(Environment environment, ClassLoader classLoader) {
			String driverClassName = environment == null ? null : environment
					.getProperty(CONFIGURATION_PREFIX + ".driverClassName");
			if (driverClassName == null) {
				driverClassName = EmbeddedDatabaseConnection.get(classLoader)
						.getDriverClassName();
			}
			return driverClassName;
		}

		private String getUrl(Environment environment, ClassLoader classLoader) {
			String url = (environment == null ? null : environment
					.getProperty(CONFIGURATION_PREFIX + ".url"));
			if (url == null) {
				url = EmbeddedDatabaseConnection.get(classLoader).getUrl();
			}
			return url;
		}
	}

	static class BasicDatabaseCondition extends NonEmbeddedDatabaseCondition {

		private Condition tomcatCondition = new TomcatDatabaseCondition();

		@Override
		protected String getDataSourceClassName() {
			return "org.apache.commons.dbcp.BasicDataSource";
		}

		@Override
		public Outcome getMatchOutcome(ConditionContext context,
				AnnotatedTypeMetadata metadata) {
			if (matches(context, metadata, this.tomcatCondition)) {
				return Outcome.noMatch("tomcat DataSource");
			}
			return super.getMatchOutcome(context, metadata);
		}
	}

	static class TomcatDatabaseCondition extends NonEmbeddedDatabaseCondition {

		@Override
		protected String getDataSourceClassName() {
			return "org.apache.tomcat.jdbc.pool.DataSource";
		}

	}

	static class EmbeddedDatabaseCondition extends SpringBootCondition {

		private SpringBootCondition tomcatCondition = new TomcatDatabaseCondition();

		private SpringBootCondition dbcpCondition = new BasicDatabaseCondition();

		@Override
		public Outcome getMatchOutcome(ConditionContext context,
				AnnotatedTypeMetadata metadata) {
			if (anyMatches(context, metadata, this.tomcatCondition, this.dbcpCondition)) {
				return Outcome.noMatch("existing non-embedded database detected");
			}
			EmbeddedDatabaseType type = EmbeddedDatabaseConnection.get(
					context.getClassLoader()).getType();
			if (type == null) {
				return Outcome.noMatch("no embedded database detected");
			}
			return Outcome.match("embedded database " + type + " detected");
		}
	}

	static class DatabaseCondition extends SpringBootCondition {

		private SpringBootCondition tomcatCondition = new TomcatDatabaseCondition();

		private SpringBootCondition dbcpCondition = new BasicDatabaseCondition();

		private SpringBootCondition embeddedCondition = new EmbeddedDatabaseCondition();

		@Override
		public Outcome getMatchOutcome(ConditionContext context,
				AnnotatedTypeMetadata metadata) {

			if (anyMatches(context, metadata, this.tomcatCondition, this.dbcpCondition,
					this.embeddedCondition)) {
				return Outcome.match("existing auto database detected");
			}

			if (BeanFactoryUtils.beanNamesForTypeIncludingAncestors(
					context.getBeanFactory(), DataSource.class, true, false).length > 0) {
				return Outcome.match("Existing bean configured database detected");
			}

			return Outcome.noMatch("no existing bean configured database");
		}
	}
}
