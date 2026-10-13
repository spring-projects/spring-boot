/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jdbc.autoconfigure;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link EmbeddedDataSourceConfiguration}.
 *
 * @author Dave Syer
 * @author Stephane Nicoll
 */
class EmbeddedDataSourceConfigurationTests {

	private @Nullable AnnotationConfigApplicationContext context;

	@AfterEach
	void closeContext() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	void defaultEmbeddedDatabase() {
		this.context = load();
		assertThat(this.context.getBean(DataSource.class)).isNotNull();
	}

	@Test
	void generateUniqueName() throws Exception {
		this.context = load("spring.datasource.generate-unique-name=true");
		try (AnnotationConfigApplicationContext context2 = load("spring.datasource.generate-unique-name=true")) {
			DataSource dataSource = this.context.getBean(DataSource.class);
			DataSource dataSource2 = context2.getBean(DataSource.class);
			assertThat(getDatabaseName(dataSource)).isNotEqualTo(getDatabaseName(dataSource2));
		}
	}

	private String getDatabaseName(DataSource dataSource) throws SQLException {
		try (Connection connection = dataSource.getConnection()) {
			ResultSet catalogs = connection.getMetaData().getCatalogs();
			if (catalogs.next()) {
				return catalogs.getString(1);
			}
			else {
				throw new IllegalStateException("Unable to get database name");
			}
		}
	}

	private AnnotationConfigApplicationContext load(String... environment) {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		TestPropertyValues.of(environment).applyTo(ctx);
		ctx.register(EmbeddedDataSourceConfiguration.class);
		ctx.refresh();
		return ctx;
	}

}
