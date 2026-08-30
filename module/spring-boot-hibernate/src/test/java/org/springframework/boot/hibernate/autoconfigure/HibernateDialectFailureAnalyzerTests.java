/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.hibernate.autoconfigure;

import java.sql.SQLException;

import javax.sql.DataSource;

import org.hibernate.HibernateException;
import org.hibernate.service.spi.ServiceException;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.TestAutoConfigurationPackage;
import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.transaction.autoconfigure.TransactionAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link HibernateDialectFailureAnalyzer}.
 *
 * @author Han Eui Jun
 */
class HibernateDialectFailureAnalyzerTests {

	private final HibernateDialectFailureAnalyzer analyzer = new HibernateDialectFailureAnalyzer();

	@Test
	void failureAnalysisIsPerformedForHibernateDialectFailure() {
		FailureAnalysis analysis = this.analyzer
			.analyze(new ServiceException("Unable to create requested service", new HibernateException(
					"Unable to determine Dialect without JDBC metadata (please set 'hibernate.dialect')")));
		assertThat(analysis).isNotNull();
		assertThat(analysis.getDescription())
			.isEqualTo("Hibernate could not determine the dialect because it could not obtain JDBC metadata.");
		assertThat(analysis.getAction()).contains("JDBC URL", "username", "password", "driver", "database is available",
				"spring.jpa.database-platform");
	}

	@Test
	void failureAnalysisIsPerformedWhenFailureIsNestedInBeanCreationException() {
		BeanCreationException failure = new BeanCreationException("entityManagerFactory",
				new ServiceException("Unable to create requested service",
						new HibernateException("Unable to determine Dialect without JDBC metadata")));
		assertThat(this.analyzer.analyze(failure)).isNotNull();
	}

	@Test
	void unrelatedHibernateFailureIsSkipped() {
		FailureAnalysis analysis = this.analyzer.analyze(new ServiceException("Unable to create requested service",
				new HibernateException("A different failure")));
		assertThat(analysis).isNull();
	}

	@Test
	void unrelatedJdbcFailureIsSkipped() {
		FailureAnalysis analysis = this.analyzer
			.analyze(new BeanCreationException("entityManagerFactory", new SQLException("Connection refused")));
		assertThat(analysis).isNull();
	}

	@Test
	void actualJpaBootstrapFailureIsAnalyzed() {
		new ApplicationContextRunner().withUserConfiguration(UnavailableDataSourceConfiguration.class)
			.withConfiguration(
					AutoConfigurations.of(TransactionAutoConfiguration.class, HibernateJpaAutoConfiguration.class))
			.run((context) -> {
				assertThat(context).hasFailed();
				Throwable startupFailure = context.getStartupFailure();
				assertThat(startupFailure).isNotNull();
				FailureAnalysis analysis = this.analyzer.analyze(startupFailure);
				assertThat(analysis).isNotNull();
				assertThat(analysis.getCause()).isInstanceOf(ServiceException.class);
			});
	}

	@Test
	void contextStartsWhenDialectIsConfigured() {
		new ApplicationContextRunner()
			.withPropertyValues("spring.jpa.database-platform=org.hibernate.dialect.H2Dialect")
			.withUserConfiguration(UnavailableDataSourceConfiguration.class)
			.withConfiguration(
					AutoConfigurations.of(TransactionAutoConfiguration.class, HibernateJpaAutoConfiguration.class))
			.run((context) -> assertThat(context).hasNotFailed());
	}

	@Configuration(proxyBeanMethods = false)
	@TestAutoConfigurationPackage(HibernateDialectFailureAnalyzerTests.class)
	static class UnavailableDataSourceConfiguration {

		@Bean
		DataSource dataSource() {
			DataSource dataSource = mock(DataSource.class);
			try {
				given(dataSource.getConnection()).willThrow(new SQLException("Connection refused"));
			}
			catch (SQLException ex) {
				throw new IllegalStateException(ex);
			}
			return dataSource;
		}

	}

}
