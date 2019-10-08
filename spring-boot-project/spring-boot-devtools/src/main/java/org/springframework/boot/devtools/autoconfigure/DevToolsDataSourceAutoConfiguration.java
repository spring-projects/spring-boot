/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.devtools.autoconfigure;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import javax.sql.DataSource;

import org.apache.derby.jdbc.EmbeddedDriver;

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
import org.springframework.boot.devtools.autoconfigure.DevToolsDataSourceAutoConfiguration.DatabaseShutdownExecutorEntityManagerFactoryDependsOnPostProcessor;
import org.springframework.boot.devtools.autoconfigure.DevToolsDataSourceAutoConfiguration.DevToolsDataSourceCondition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ConfigurationCondition;
import org.springframework.context.annotation.Import;
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
@Conditional({ OnEnabledDevToolsCondition.class, DevToolsDataSourceCondition.class })
@Configuration(proxyBeanMethods = false)
@Import(DatabaseShutdownExecutorEntityManagerFactoryDependsOnPostProcessor.class)
public class DevToolsDataSourceAutoConfiguration {

	@Bean
	NonEmbeddedInMemoryDatabaseShutdownExecutor inMemoryDatabaseShutdownExecutor(DataSource dataSource,
			DataSourceProperties dataSourceProperties) {
		return new NonEmbeddedInMemoryDatabaseShutdownExecutor(dataSource, dataSourceProperties);
	}

	/**
	 * Post processor to ensure that {@link javax.persistence.EntityManagerFactory} beans
	 * depend on the {@code inMemoryDatabaseShutdownExecutor} bean.
	 */
	@ConditionalOnClass(LocalContainerEntityManagerFactoryBean.class)
	@ConditionalOnBean(AbstractEntityManagerFactoryBean.class)
	static class DatabaseShutdownExecutorEntityManagerFactoryDependsOnPostProcessor
			extends EntityManagerFactoryDependsOnPostProcessor {

		DatabaseShutdownExecutorEntityManagerFactoryDependsOnPostProcessor() {
			super("inMemoryDatabaseShutdownExecutor");
		}

	}

	static final class NonEmbeddedInMemoryDatabaseShutdownExecutor implements DisposableBean {

		private final DataSource dataSource;

		private final DataSourceProperties dataSourceProperties;

		NonEmbeddedInMemoryDatabaseShutdownExecutor(DataSource dataSource, DataSourceProperties dataSourceProperties) {
			this.dataSource = dataSource;
			this.dataSourceProperties = dataSourceProperties;
		}

		@Override
		public void destroy() throws Exception {
			for (InMemoryDatabase inMemoryDatabase : InMemoryDatabase.values()) {
				if (inMemoryDatabase.matches(this.dataSourceProperties)) {
					inMemoryDatabase.shutdown(this.dataSource);
					return;
				}
			}
		}

		private enum InMemoryDatabase {

			DERBY(null, new HashSet<>(Arrays.asList("org.apache.derby.jdbc.EmbeddedDriver")), (dataSource) -> {
				String url = dataSource.getConnection().getMetaData().getURL();
				try {
					new EmbeddedDriver().connect(url + ";drop=true", new Properties());
				}
				catch (SQLException ex) {
					if (!"08006".equals(ex.getSQLState())) {
						throw ex;
					}
				}
			}),

			H2("jdbc:h2:mem:", new HashSet<>(Arrays.asList("org.h2.Driver", "org.h2.jdbcx.JdbcDataSource"))),

			HSQLDB("jdbc:hsqldb:mem:", new HashSet<>(Arrays.asList("org.hsqldb.jdbcDriver",
					"org.hsqldb.jdbc.JDBCDriver", "org.hsqldb.jdbc.pool.JDBCXADataSource")));

			private final String urlPrefix;

			private final ShutdownHandler shutdownHandler;

			private final Set<String> driverClassNames;

			InMemoryDatabase(String urlPrefix, Set<String> driverClassNames) {
				this(urlPrefix, driverClassNames, (dataSource) -> {
					try (Connection connection = dataSource.getConnection()) {
						try (Statement statement = connection.createStatement()) {
							statement.execute("SHUTDOWN");
						}
					}
				});
			}

			InMemoryDatabase(String urlPrefix, Set<String> driverClassNames, ShutdownHandler shutdownHandler) {
				this.urlPrefix = urlPrefix;
				this.driverClassNames = driverClassNames;
				this.shutdownHandler = shutdownHandler;
			}

			boolean matches(DataSourceProperties properties) {
				String url = properties.getUrl();
				return (url == null || this.urlPrefix == null || url.startsWith(this.urlPrefix))
						&& this.driverClassNames.contains(properties.determineDriverClassName());
			}

			void shutdown(DataSource dataSource) throws SQLException {
				this.shutdownHandler.shutdown(dataSource);
			}

			@FunctionalInterface
			interface ShutdownHandler {

				void shutdown(DataSource dataSource) throws SQLException;

			}

		}

	}

	static class DevToolsDataSourceCondition extends SpringBootCondition implements ConfigurationCondition {

		@Override
		public ConfigurationPhase getConfigurationPhase() {
			return ConfigurationPhase.REGISTER_BEAN;
		}

		@Override
		public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
			ConditionMessage.Builder message = ConditionMessage.forCondition("DevTools DataSource Condition");
			String[] dataSourceBeanNames = context.getBeanFactory().getBeanNamesForType(DataSource.class);
			if (dataSourceBeanNames.length != 1) {
				return ConditionOutcome.noMatch(message.didNotFind("a single DataSource bean").atAll());
			}
			if (context.getBeanFactory().getBeanNamesForType(DataSourceProperties.class).length != 1) {
				return ConditionOutcome.noMatch(message.didNotFind("a single DataSourceProperties bean").atAll());
			}
			BeanDefinition dataSourceDefinition = context.getRegistry().getBeanDefinition(dataSourceBeanNames[0]);
			if (dataSourceDefinition instanceof AnnotatedBeanDefinition
					&& ((AnnotatedBeanDefinition) dataSourceDefinition).getFactoryMethodMetadata() != null
					&& ((AnnotatedBeanDefinition) dataSourceDefinition).getFactoryMethodMetadata()
							.getDeclaringClassName().startsWith(DataSourceAutoConfiguration.class.getPackage().getName()
									+ ".DataSourceConfiguration$")) {
				return ConditionOutcome.match(message.foundExactly("auto-configured DataSource"));
			}
			return ConditionOutcome.noMatch(message.didNotFind("an auto-configured DataSource").atAll());
		}

	}

}
