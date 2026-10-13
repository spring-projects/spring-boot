/*
 * Copyright 2012-present the original author or authors.
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
