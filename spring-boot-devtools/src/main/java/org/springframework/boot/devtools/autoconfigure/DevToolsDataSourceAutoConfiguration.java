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

package org.springframework.boot.devtools.autoconfigure;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.sql.DataSource;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionMessage;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.boot.autoconfigure.data.jpa.EntityManagerFactoryDependsOnPostProcessor;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.devtools.autoconfigure.DevToolsDataSourceAutoConfiguration.DevToolsDataSourceCondition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ConfigurationCondition;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.orm.jpa.AbstractEntityManagerFactoryBean;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for DevTools-specific
 * {@link DataSource} configuration.
 *
 * @author Andy Wilkinson
 * @since 1.3.3
 */
@AutoConfigureAfter(DataSourceAutoConfiguration.class)
@Conditional(DevToolsDataSourceCondition.class)
@Configuration
public class DevToolsDataSourceAutoConfiguration {

	@Bean
	NonEmbeddedInMemoryDatabaseShutdownExecutor inMemoryDatabaseShutdownExecutor(
			DataSource dataSource, DataSourceProperties dataSourceProperties) {
		return new NonEmbeddedInMemoryDatabaseShutdownExecutor(dataSource,
				dataSourceProperties);
	}

	/**
	 * Additional configuration to ensure that
	 * {@link javax.persistence.EntityManagerFactory} beans depend on the
	 * {@code inMemoryDatabaseShutdownExecutor} bean.
	 */
	@Configuration
	@ConditionalOnClass(LocalContainerEntityManagerFactoryBean.class)
	@ConditionalOnBean(AbstractEntityManagerFactoryBean.class)
	static class DatabaseShutdownExecutorJpaDependencyConfiguration
			extends EntityManagerFactoryDependsOnPostProcessor {

		DatabaseShutdownExecutorJpaDependencyConfiguration() {
			super("inMemoryDatabaseShutdownExecutor");
		}

	}

	static final class NonEmbeddedInMemoryDatabaseShutdownExecutor
			implements DisposableBean {

		private final DataSource dataSource;

		private final DataSourceProperties dataSourceProperties;

		NonEmbeddedInMemoryDatabaseShutdownExecutor(DataSource dataSource,
				DataSourceProperties dataSourceProperties) {
			this.dataSource = dataSource;
			this.dataSourceProperties = dataSourceProperties;
		}

		@Override
		public void destroy() throws Exception {
			if (dataSourceRequiresShutdown()) {
				this.dataSource.getConnection().createStatement().execute("SHUTDOWN");
			}
		}

		private boolean dataSourceRequiresShutdown() {
			for (InMemoryDatabase inMemoryDatabase : InMemoryDatabase.values()) {
				if (inMemoryDatabase.matches(this.dataSourceProperties)) {
					return true;
				}
			}
			return false;
		}

		private enum InMemoryDatabase {

			DERBY(null, "org.apache.derby.jdbc.EmbeddedDriver"),

			H2("jdbc:h2:mem:", "org.h2.Driver", "org.h2.jdbcx.JdbcDataSource"),

			HSQLDB("jdbc:hsqldb:mem:", "org.hsqldb.jdbcDriver",
					"org.hsqldb.jdbc.JDBCDriver",
					"org.hsqldb.jdbc.pool.JDBCXADataSource");

			private final String urlPrefix;

			private final Set<String> driverClassNames;

			InMemoryDatabase(String urlPrefix, String... driverClassNames) {
				this.urlPrefix = urlPrefix;
				this.driverClassNames = new HashSet<>(Arrays.asList(driverClassNames));
			}

			boolean matches(DataSourceProperties properties) {
				String url = properties.getUrl();
				return (url == null || this.urlPrefix == null
						|| url.startsWith(this.urlPrefix))
						&& this.driverClassNames
								.contains(properties.determineDriverClassName());
			}

		}

	}

	static class DevToolsDataSourceCondition extends SpringBootCondition
			implements ConfigurationCondition {

		@Override
		public ConfigurationPhase getConfigurationPhase() {
			return ConfigurationPhase.REGISTER_BEAN;
		}

		@Override
		public ConditionOutcome getMatchOutcome(ConditionContext context,
				AnnotatedTypeMetadata metadata) {
			ConditionMessage.Builder message = ConditionMessage
					.forCondition("DevTools DataSource Condition");
			String[] dataSourceBeanNames = context.getBeanFactory()
					.getBeanNamesForType(DataSource.class);
			if (dataSourceBeanNames.length != 1) {
				return ConditionOutcome
						.noMatch(message.didNotFind("a single DataSource bean").atAll());
			}
			if (context.getBeanFactory()
					.getBeanNamesForType(DataSourceProperties.class).length != 1) {
				return ConditionOutcome.noMatch(
						message.didNotFind("a single DataSourceProperties bean").atAll());
			}
			BeanDefinition dataSourceDefinition = context.getRegistry()
					.getBeanDefinition(dataSourceBeanNames[0]);
			if (dataSourceDefinition instanceof AnnotatedBeanDefinition
					&& ((AnnotatedBeanDefinition) dataSourceDefinition)
							.getFactoryMethodMetadata() != null
					&& ((AnnotatedBeanDefinition) dataSourceDefinition)
							.getFactoryMethodMetadata().getDeclaringClassName()
							.startsWith(DataSourceAutoConfiguration.class.getPackage()
									.getName() + ".DataSourceConfiguration$")) {
				return ConditionOutcome
						.match(message.foundExactly("auto-configured DataSource"));
			}
			return ConditionOutcome
					.noMatch(message.didNotFind("an auto-configured DataSource").atAll());
		}

	}

}
