/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jdbc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.sql.DataSource;

import org.jspecify.annotations.Nullable;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

/**
 * {@link RuntimeHintsRegistrar} implementation for {@link DataSource} types supported by
 * the {@link DataSourceBuilder}.
 *
 * @author Phillip Webb
 */
class DataSourceBuilderRuntimeHints implements RuntimeHintsRegistrar {

	private static final List<String> TYPE_NAMES;
	static {
		List<String> typeNames = new ArrayList<>();
		typeNames.add("com.mchange.v2.c3p0.ComboPooledDataSource");
		typeNames.add("com.zaxxer.hikari.HikariDataSource");
		typeNames.add("oracle.jdbc.datasource.OracleDataSource");
		typeNames.add("oracle.ucp.jdbc.PoolDataSourceImpl");
		typeNames.add("org.apache.commons.dbcp2.BasicDataSource");
		typeNames.add("org.apache.tomcat.jdbc.pool.DataSource");
		typeNames.add("org.h2.jdbcx.JdbcDataSource");
		typeNames.add("org.postgresql.ds.PGSimpleDataSource");
		typeNames.add("org.springframework.jdbc.datasource.SimpleDriverDataSource");
		typeNames.add("org.vibur.dbcp.ViburDBCPDataSource");
		TYPE_NAMES = Collections.unmodifiableList(typeNames);
	}

	@Override
	public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
		for (String typeName : TYPE_NAMES) {
			hints.reflection()
				.registerTypeIfPresent(classLoader, typeName,
						(hint) -> hint.withMembers(MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS));
		}
	}

}
