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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.context.ApplicationContext;
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
@ConditionalOnMissingBean(DataSource.class)
public class DataSourceAutoConfiguration {

	private static Log logger = LogFactory.getLog(DataSourceAutoConfiguration.class);

	@Autowired(required = false)
	private DataSource dataSource;

	@Autowired
	private ApplicationContext applicationContext;

	@Conditional(DataSourceAutoConfiguration.EmbeddedDatabaseCondition.class)
	@Import(EmbeddedDatabaseConfiguration.class)
	protected static class EmbeddedConfiguration {
	}

	@Conditional(DataSourceAutoConfiguration.TomcatDatabaseCondition.class)
	@Import(TomcatDataSourceConfiguration.class)
	protected static class TomcatConfiguration {
	}

	@Conditional(DataSourceAutoConfiguration.BasicDatabaseCondition.class)
	@Import(BasicDataSourceConfiguration.class)
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

	@Value("${spring.database.schema:classpath*:schema-${spring.database.platform:all}.sql}")
	private String schemaLocations = "";

	@PostConstruct
	protected void initialize() throws Exception {
		if (this.dataSource == null) {
			logger.debug("No DataSource found so not initializing");
			return;
		}
		ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
		boolean exists = false;
		List<Resource> resources = new ArrayList<Resource>();
		for (String location : StringUtils
				.commaDelimitedListToStringArray(this.schemaLocations)) {
			resources
					.addAll(Arrays.asList(this.applicationContext.getResources(location)));
		}
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

	static abstract class NonEmbeddedDatabaseCondition extends SpringBootCondition {

		protected abstract String getDataSourceClassName();

		@Override
		public Outcome getMatchOutcome(ConditionContext context,
				AnnotatedTypeMetadata metadata) {

			if (!ClassUtils.isPresent(getDataSourceClassName(), context.getClassLoader())) {
				return Outcome.noMatch(getDataSourceClassName()
						+ " DataSource class not found");
			}

			String driverClassName = getDriverClassName(context.getEnvironment());
			if (driverClassName == null) {
				return Outcome.noMatch("no database driver");
			}

			String url = getUrl(context.getEnvironment());
			if (url == null) {
				return Outcome.noMatch("no database URL");
			}

			if (ClassUtils.isPresent(driverClassName, context.getClassLoader())) {
				return Outcome.match("found database driver " + driverClassName);
			}

			return Outcome.match("missing database driver " + driverClassName);
		}

		private String getDriverClassName(Environment environment) {
			String driverClassName = environment == null ? null : environment
					.getProperty("spring.database.driverClassName");
			if (driverClassName == null) {
				driverClassName = EmbeddedDatabaseConfiguration
						.getEmbeddedDatabaseDriverClass(EmbeddedDatabaseConfiguration
								.getEmbeddedDatabaseType());
			}
			return driverClassName;
		}

		private String getUrl(Environment environment) {
			String url = (environment == null ? null : environment
					.getProperty("spring.database.url"));
			if (url == null) {
				url = EmbeddedDatabaseConfiguration
						.getEmbeddedDatabaseUrl(EmbeddedDatabaseConfiguration
								.getEmbeddedDatabaseType());
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
			EmbeddedDatabaseType type = EmbeddedDatabaseConfiguration
					.getEmbeddedDatabaseType();
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
