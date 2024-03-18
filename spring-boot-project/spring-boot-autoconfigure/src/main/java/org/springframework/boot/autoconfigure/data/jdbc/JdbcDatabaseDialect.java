/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.autoconfigure.data.jdbc;

import java.util.function.Supplier;

import org.springframework.data.jdbc.core.dialect.JdbcDb2Dialect;
import org.springframework.data.jdbc.core.dialect.JdbcMySqlDialect;
import org.springframework.data.jdbc.core.dialect.JdbcPostgresDialect;
import org.springframework.data.jdbc.core.dialect.JdbcSqlServerDialect;
import org.springframework.data.relational.core.dialect.Db2Dialect;
import org.springframework.data.relational.core.dialect.Dialect;
import org.springframework.data.relational.core.dialect.H2Dialect;
import org.springframework.data.relational.core.dialect.HsqlDbDialect;
import org.springframework.data.relational.core.dialect.MariaDbDialect;
import org.springframework.data.relational.core.dialect.OracleDialect;

/**
 * List of database dialects that can be configured in Boot for use with Spring Data JDBC.
 *
 * @author Jens Schauder
 * @since 3.3
 */
public enum JdbcDatabaseDialect implements Supplier<Dialect> {

	DB2 {
		@Override
		public Dialect get() {
			return JdbcDb2Dialect.INSTANCE;
		}
	},
	H2{
		@Override
		public Dialect get() {
			return H2Dialect.INSTANCE;
		}
	},
	HSQL{
		@Override
		public Dialect get() {
			return HsqlDbDialect.INSTANCE;
		}
	},
	MARIA{
		@Override
		public Dialect get() {
			return MariaDbDialect.INSTANCE;
		}
	},
	MYSQL{
		@Override
		public Dialect get() {
			return JdbcMySqlDialect.INSTANCE;
		}
	},
	ORACLE{
		@Override
		public Dialect get() {
			return OracleDialect.INSTANCE;

		}
	},
	POSTGRESQL{
		@Override
		public Dialect get() {
			return JdbcPostgresDialect.INSTANCE;
		}
	},
	SQL_SERVER{
		@Override
		public Dialect get() {
			return JdbcSqlServerDialect.INSTANCE;
		}
	}
}
