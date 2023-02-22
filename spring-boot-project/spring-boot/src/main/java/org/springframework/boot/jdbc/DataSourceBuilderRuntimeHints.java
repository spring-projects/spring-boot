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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.sql.DataSource;

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
		typeNames.add("org.h2.jdbcx.JdbcDataSource");
		typeNames.add("com.zaxxer.hikari.HikariDataSource");
		typeNames.add("org.apache.commons.dbcp2.BasicDataSource");
		typeNames.add("oracle.jdbc.datasource.OracleDataSource");
		typeNames.add("oracle.ucp.jdbc.PoolDataSource");
		typeNames.add("org.postgresql.ds.PGSimpleDataSource");
		typeNames.add("org.springframework.jdbc.datasource.SimpleDriverDataSource");
		typeNames.add("org.apache.tomcat.jdbc.pool.DataSource");
		TYPE_NAMES = Collections.unmodifiableList(typeNames);
	}

	@Override
	public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
		for (String typeName : TYPE_NAMES) {
			hints.reflection()
				.registerTypeIfPresent(classLoader, typeName,
						(hint) -> hint.withMembers(MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS));
		}
	}

}
