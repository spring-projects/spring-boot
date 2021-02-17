/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.autoconfigure.jdbc;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.boot.autoconfigure.orm.jpa.EntityManagerFactoryDependsOnPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;

/**
 * Configuration for {@link DataSource} initialization using DDL and DML scripts.
 *
 * @author Andy Wilkinson
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnSingleCandidate(DataSource.class)
class DataSourceInitializationConfiguration {

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnProperty(prefix = "spring.datasource", name = "initialization-order", havingValue = "before-jpa",
			matchIfMissing = true)
	@Import({ DataSourceInitializationJdbcOperationsDependsOnPostProcessor.class,
			DataSourceInitializationNamedParameterJdbcOperationsDependsOnPostProcessor.class,
			DataSourceInitializationEntityManagerFactoryDependsOnPostProcessor.class })
	static class BeforeJpaDataSourceInitializationConfiguration {

		@Bean
		DataSourceInitialization dataSourceInitialization(DataSource dataSource, DataSourceProperties properties) {
			return new DataSourceInitialization(dataSource, properties);
		}

	}

	/**
	 * Post processor to ensure that {@link EntityManagerFactory} beans depend on any
	 * {@link DataSourceInitialization} beans.
	 */
	@ConditionalOnClass({ LocalContainerEntityManagerFactoryBean.class, EntityManagerFactory.class })
	static class DataSourceInitializationEntityManagerFactoryDependsOnPostProcessor
			extends EntityManagerFactoryDependsOnPostProcessor {

		DataSourceInitializationEntityManagerFactoryDependsOnPostProcessor() {
			super(DataSourceInitialization.class);
		}

	}

	/**
	 * Post processor to ensure that {@link JdbcOperations} beans depend on any
	 * {@link DataSourceInitialization} beans.
	 */
	@ConditionalOnClass(JdbcOperations.class)
	static class DataSourceInitializationJdbcOperationsDependsOnPostProcessor
			extends JdbcOperationsDependsOnPostProcessor {

		DataSourceInitializationJdbcOperationsDependsOnPostProcessor() {
			super(DataSourceInitialization.class);
		}

	}

	/**
	 * Post processor to ensure that {@link NamedParameterJdbcOperations} beans depend on
	 * any {@link DataSourceInitialization} beans.
	 */
	@ConditionalOnClass(NamedParameterJdbcOperations.class)
	protected static class DataSourceInitializationNamedParameterJdbcOperationsDependsOnPostProcessor
			extends NamedParameterJdbcOperationsDependsOnPostProcessor {

		public DataSourceInitializationNamedParameterJdbcOperationsDependsOnPostProcessor() {
			super(DataSourceInitialization.class);
		}

	}

}
