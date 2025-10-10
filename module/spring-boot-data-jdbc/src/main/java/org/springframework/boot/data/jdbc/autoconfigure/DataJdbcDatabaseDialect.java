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

package org.springframework.boot.data.jdbc.autoconfigure;

import java.util.function.Function;

import org.springframework.data.jdbc.core.dialect.DialectResolver;
import org.springframework.data.jdbc.core.dialect.JdbcDb2Dialect;
import org.springframework.data.jdbc.core.dialect.JdbcH2Dialect;
import org.springframework.data.jdbc.core.dialect.JdbcHsqlDbDialect;
import org.springframework.data.jdbc.core.dialect.JdbcMariaDbDialect;
import org.springframework.data.jdbc.core.dialect.JdbcMySqlDialect;
import org.springframework.data.jdbc.core.dialect.JdbcOracleDialect;
import org.springframework.data.jdbc.core.dialect.JdbcPostgresDialect;
import org.springframework.data.jdbc.core.dialect.JdbcSqlServerDialect;
import org.springframework.data.relational.core.dialect.Dialect;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.util.Assert;

/**
 * List of database dialects that can be configured in Boot for use with Spring Data JDBC.
 *
 * @author Jens Schauder
 * @since 4.0.0
 */
public enum DataJdbcDatabaseDialect {

	/**
	 * Provides an instance of {@link JdbcDb2Dialect}.
	 */
	DB2(JdbcDb2Dialect.INSTANCE),

	/**
	 * Provides an instance of {@link JdbcH2Dialect}.
	 */
	H2(JdbcH2Dialect.INSTANCE),

	/**
	 * Provides an instance of {@link JdbcHsqlDbDialect}.
	 */
	HSQL(JdbcHsqlDbDialect.INSTANCE),

	/**
	 * Resolves an instance of {@link JdbcMariaDbDialect} by querying the database
	 * configuration.
	 */
	MARIA(JdbcMariaDbDialect.class),

	/**
	 * Resolves an instance of {@link JdbcMySqlDialect} by querying the database
	 * configuration.
	 */
	MYSQL(JdbcMySqlDialect.class),

	/**
	 * Provides an instance of {@link JdbcOracleDialect}.
	 */
	ORACLE(JdbcOracleDialect.INSTANCE),

	/**
	 * Provides an instance of {@link JdbcPostgresDialect}.
	 */
	POSTGRESQL(JdbcPostgresDialect.INSTANCE),

	/**
	 * Provides an instance of {@link JdbcSqlServerDialect}.
	 */
	SQL_SERVER(JdbcSqlServerDialect.INSTANCE);

	private final Function<JdbcOperations, Dialect> dialectResolver;

	DataJdbcDatabaseDialect(Class<? extends Dialect> dialectType) {
		this.dialectResolver = (jdbc) -> {
			Dialect dialect = DialectResolver.getDialect(jdbc);
			Assert.isInstanceOf(dialectType, dialect);
			return dialect;
		};
	}

	DataJdbcDatabaseDialect(Dialect dialect) {
		this.dialectResolver = (jdbc) -> dialect;
	}

	Dialect getDialect(JdbcOperations jdbc) {
		return this.dialectResolver.apply(jdbc);
	}

}
