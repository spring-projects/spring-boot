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

package org.springframework.boot.liquibase;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariDataSource;
import liquibase.integration.spring.SpringLiquibase;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link LiquibaseChangelogMissingFailureAnalyzer}
 *
 * @author Sebastiaan Fernandez
 */
class LiquibaseChangelogMissingFailureAnalyzerTests {

	@Test
	void changelogParseExceptionDueToChangelogNotPresent() {
		FailureAnalysis analysis = performAnalysis();
		assertThat(analysis).isNotNull();
		assertThat(analysis.getDescription())
			.isEqualTo("Liquibase failed to start because no changelog could be found at '"
					+ "classpath:/db/changelog/db.changelog-master.yaml'.");
		assertThat(analysis.getAction())
			.isEqualTo("Make sure a Liquibase changelog is present at the configured path.");
	}

	private @Nullable FailureAnalysis performAnalysis() {
		BeanCreationException failure = createFailure();
		assertThat(failure).isNotNull();
		return new LiquibaseChangelogMissingFailureAnalyzer().analyze(failure);
	}

	private @Nullable BeanCreationException createFailure() {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				LiquibaseConfiguration.class)) {
			return null;
		}
		catch (BeanCreationException ex) {
			return ex;
		}
	}

	@Configuration(proxyBeanMethods = false)
	static class LiquibaseConfiguration {

		@Bean
		DataSource dataSource() {
			HikariDataSource dataSource = new HikariDataSource();
			dataSource.setJdbcUrl("jdbc:h2:mem:test");
			dataSource.setUsername("sa");
			return dataSource;
		}

		@Bean
		SpringLiquibase springLiquibase(DataSource dataSource) {
			SpringLiquibase liquibase = new SpringLiquibase();
			liquibase.setChangeLog("classpath:/db/changelog/db.changelog-master.yaml");
			liquibase.setDataSource(dataSource);
			return liquibase;
		}

	}

}
