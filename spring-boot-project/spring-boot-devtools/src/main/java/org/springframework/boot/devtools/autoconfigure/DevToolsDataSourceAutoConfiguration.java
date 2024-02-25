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

package org.springframework.boot.devtools.autoconfigure;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.Set;

import javax.sql.DataSource;

import org.apache.derby.jdbc.EmbeddedDriver;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionMessage;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.autoconfigure.orm.jpa.EntityManagerFactoryDependsOnPostProcessor;
import org.springframework.boot.devtools.autoconfigure.DevToolsDataSourceAutoConfiguration.DatabaseShutdownExecutorEntityManagerFactoryDependsOnPostProcessor;
import org.springframework.boot.devtools.autoconfigure.DevToolsDataSourceAutoConfiguration.DevToolsDataSourceCondition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
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
@Conditional({ OnEnabledDevToolsCondition.class, DevToolsDataSourceCondition.class })
@AutoConfiguration(after = DataSourceAutoConfiguration.class)
@Import(DatabaseShutdownExecutorEntityManagerFactoryDependsOnPostProcessor.class)
public class DevToolsDataSourceAutoConfiguration {

	/**
     * Creates a NonEmbeddedInMemoryDatabaseShutdownExecutor bean.
     * 
     * @param dataSource the DataSource bean used for the in-memory database
     * @param dataSourceProperties the DataSourceProperties bean containing the properties for the in-memory database
     * @return the NonEmbeddedInMemoryDatabaseShutdownExecutor bean
     */
    @Bean
	NonEmbeddedInMemoryDatabaseShutdownExecutor inMemoryDatabaseShutdownExecutor(DataSource dataSource,
			DataSourceProperties dataSourceProperties) {
		return new NonEmbeddedInMemoryDatabaseShutdownExecutor(dataSource, dataSourceProperties);
	}

	/**
	 * Post processor to ensure that {@link jakarta.persistence.EntityManagerFactory}
	 * beans depend on the {@code inMemoryDatabaseShutdownExecutor} bean.
	 */
	@ConditionalOnClass(LocalContainerEntityManagerFactoryBean.class)
	@ConditionalOnBean(AbstractEntityManagerFactoryBean.class)
	static class DatabaseShutdownExecutorEntityManagerFactoryDependsOnPostProcessor
			extends EntityManagerFactoryDependsOnPostProcessor {

		/**
         * Constructs a new DatabaseShutdownExecutorEntityManagerFactoryDependsOnPostProcessor with the specified in-memory database shutdown executor.
         *
         * @param inMemoryDatabaseShutdownExecutor the in-memory database shutdown executor
         */
        DatabaseShutdownExecutorEntityManagerFactoryDependsOnPostProcessor() {
			super("inMemoryDatabaseShutdownExecutor");
		}

	}

	/**
     * NonEmbeddedInMemoryDatabaseShutdownExecutor class.
     */
    static final class NonEmbeddedInMemoryDatabaseShutdownExecutor implements DisposableBean {

		private final DataSource dataSource;

		private final DataSourceProperties dataSourceProperties;

		/**
         * Constructs a new NonEmbeddedInMemoryDatabaseShutdownExecutor with the specified DataSource and DataSourceProperties.
         * 
         * @param dataSource the DataSource to be used for shutting down the non-embedded in-memory database
         * @param dataSourceProperties the DataSourceProperties containing the configuration for the DataSource
         */
        NonEmbeddedInMemoryDatabaseShutdownExecutor(DataSource dataSource, DataSourceProperties dataSourceProperties) {
			this.dataSource = dataSource;
			this.dataSourceProperties = dataSourceProperties;
		}

		/**
         * This method is used to destroy the non-embedded in-memory database.
         * It iterates through the available in-memory databases and shuts down the one that matches the given data source properties.
         * 
         * @throws Exception if an error occurs during the shutdown process
         */
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

			DERBY(null, Set.of("org.apache.derby.jdbc.EmbeddedDriver"), (dataSource) -> {
				String url;
				try (Connection connection = dataSource.getConnection()) {
					url = connection.getMetaData().getURL();
				}
				try {
					new EmbeddedDriver().connect(url + ";drop=true", new Properties()).close();
				}
				catch (SQLException ex) {
					if (!"08006".equals(ex.getSQLState())) {
						throw ex;
					}
				}
			}),

			H2("jdbc:h2:mem:", Set.of("org.h2.Driver", "org.h2.jdbcx.JdbcDataSource")),

			HSQLDB("jdbc:hsqldb:mem:", Set.of("org.hsqldb.jdbcDriver", "org.hsqldb.jdbc.JDBCDriver",
					"org.hsqldb.jdbc.pool.JDBCXADataSource"));

			private final String urlPrefix;

			private final ShutdownHandler shutdownHandler;

			private final Set<String> driverClassNames;

			/**
         * Constructs a new InMemoryDatabase object with the specified URL prefix and driver class names.
         * 
         * @param urlPrefix the URL prefix for the database connection
         * @param driverClassNames the set of driver class names to be used for the database connection
         */
        InMemoryDatabase(String urlPrefix, Set<String> driverClassNames) {
				this(urlPrefix, driverClassNames, (dataSource) -> {
					try (Connection connection = dataSource.getConnection()) {
						try (Statement statement = connection.createStatement()) {
							statement.execute("SHUTDOWN");
						}
					}
				});
			}

			/**
         * Constructs a new InMemoryDatabase with the specified URL prefix, set of driver class names, and shutdown handler.
         * 
         * @param urlPrefix the URL prefix for the database
         * @param driverClassNames the set of driver class names to be used for the database
         * @param shutdownHandler the shutdown handler for the database
         */
        InMemoryDatabase(String urlPrefix, Set<String> driverClassNames, ShutdownHandler shutdownHandler) {
				this.urlPrefix = urlPrefix;
				this.driverClassNames = driverClassNames;
				this.shutdownHandler = shutdownHandler;
			}

			/**
         * Checks if the given DataSourceProperties object matches the current instance.
         * 
         * @param properties the DataSourceProperties object to be checked
         * @return true if the properties match, false otherwise
         */
        boolean matches(DataSourceProperties properties) {
				String url = properties.getUrl();
				return (url == null || this.urlPrefix == null || url.startsWith(this.urlPrefix))
						&& this.driverClassNames.contains(properties.determineDriverClassName());
			}

			/**
         * Shuts down the given data source.
         * 
         * @param dataSource the data source to be shut down
         * @throws SQLException if an error occurs while shutting down the data source
         */
        void shutdown(DataSource dataSource) throws SQLException {
				this.shutdownHandler.shutdown(dataSource);
			}

			@FunctionalInterface
			interface ShutdownHandler {

				void shutdown(DataSource dataSource) throws SQLException;

			}

		}

	}

	/**
     * DevToolsDataSourceCondition class.
     */
    static class DevToolsDataSourceCondition extends SpringBootCondition implements ConfigurationCondition {

		/**
         * Returns the configuration phase of this method.
         * 
         * @return The configuration phase of this method.
         */
        @Override
		public ConfigurationPhase getConfigurationPhase() {
			return ConfigurationPhase.REGISTER_BEAN;
		}

		/**
         * Determines the outcome of the condition for the DevTools DataSource.
         * 
         * @param context the condition context
         * @param metadata the annotated type metadata
         * @return the condition outcome
         */
        @Override
		public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
			ConditionMessage.Builder message = ConditionMessage.forCondition("DevTools DataSource Condition");
			String[] dataSourceBeanNames = context.getBeanFactory().getBeanNamesForType(DataSource.class, true, false);
			if (dataSourceBeanNames.length != 1) {
				return ConditionOutcome.noMatch(message.didNotFind("a single DataSource bean").atAll());
			}
			if (context.getBeanFactory().getBeanNamesForType(DataSourceProperties.class, true, false).length != 1) {
				return ConditionOutcome.noMatch(message.didNotFind("a single DataSourceProperties bean").atAll());
			}
			BeanDefinition dataSourceDefinition = context.getRegistry().getBeanDefinition(dataSourceBeanNames[0]);
			if (dataSourceDefinition instanceof AnnotatedBeanDefinition annotatedBeanDefinition
					&& annotatedBeanDefinition.getFactoryMethodMetadata() != null
					&& annotatedBeanDefinition.getFactoryMethodMetadata()
						.getDeclaringClassName()
						.startsWith(DataSourceAutoConfiguration.class.getPackage().getName()
								+ ".DataSourceConfiguration$")) {
				return ConditionOutcome.match(message.foundExactly("auto-configured DataSource"));
			}
			return ConditionOutcome.noMatch(message.didNotFind("an auto-configured DataSource").atAll());
		}

	}

}
