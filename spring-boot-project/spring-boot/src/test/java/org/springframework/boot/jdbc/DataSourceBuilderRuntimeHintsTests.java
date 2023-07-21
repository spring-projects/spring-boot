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
			.of(com.mchange.v2.c3p0.ComboPooledDataSource.class, org.h2.jdbcx.JdbcDataSource.class,
					com.zaxxer.hikari.HikariDataSource.class, org.apache.commons.dbcp2.BasicDataSource.class,
					oracle.jdbc.datasource.OracleDataSource.class, oracle.ucp.jdbc.PoolDataSource.class,
					org.postgresql.ds.PGSimpleDataSource.class,
					org.springframework.jdbc.datasource.SimpleDriverDataSource.class,
					org.apache.tomcat.jdbc.pool.DataSource.class)
			.forEach((dataSourceType) -> {
				TypeHint typeHint = hints.getTypeHint(dataSourceType);
				assertThat(typeHint).withFailMessage(() -> "No hints found for data source type " + dataSourceType)
					.isNotNull();
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
