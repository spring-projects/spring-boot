/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jdbc;

import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.ReflectionHints;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.TypeHint;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DataSourceBuilderRuntimeHints}.
 *
 * @author Phillip Webb
 */
class DataSourceBuilderRuntimeHintsTests {

	@Test
	void shouldRegisterDataSourceConstructors() {
		ReflectionHints hints = registerHints();
		Stream
			.of(com.mchange.v2.c3p0.ComboPooledDataSource.class, com.zaxxer.hikari.HikariDataSource.class,
					oracle.jdbc.datasource.OracleDataSource.class, oracle.ucp.jdbc.PoolDataSourceImpl.class,
					org.apache.commons.dbcp2.BasicDataSource.class, org.apache.tomcat.jdbc.pool.DataSource.class,
					org.h2.jdbcx.JdbcDataSource.class, org.postgresql.ds.PGSimpleDataSource.class,
					org.springframework.jdbc.datasource.SimpleDriverDataSource.class,
					org.vibur.dbcp.ViburDBCPDataSource.class)
			.forEach((dataSourceType) -> {
				TypeHint typeHint = hints.getTypeHint(dataSourceType);
				assertThat(typeHint).withFailMessage(() -> "No hints found for data source type " + dataSourceType)
					.isNotNull();
				assertThat(typeHint).isNotNull();
				Set<MemberCategory> memberCategories = typeHint.getMemberCategories();
				assertThat(memberCategories).containsExactly(MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS);
			});
	}

	private ReflectionHints registerHints() {
		RuntimeHints hints = new RuntimeHints();
		new DataSourceBuilderRuntimeHints().registerHints(hints, getClass().getClassLoader());
		return hints.reflection();
	}

}
