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

package org.springframework.boot.jooq.autoconfigure;

import java.sql.SQLException;
import java.sql.SQLSyntaxErrorException;
import java.util.function.Function;

import org.jooq.Configuration;
import org.jooq.ExecuteContext;
import org.jooq.SQLDialect;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.support.SQLExceptionTranslator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.assertArg;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

/**
 * Tests for {@link DefaultExceptionTranslatorExecuteListener}.
 *
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class DefaultExceptionTranslatorExecuteListenerTests {

	private final ExceptionTranslatorExecuteListener listener = new DefaultExceptionTranslatorExecuteListener();

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void createWhenTranslatorFactoryIsNullThrowsException() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> new DefaultExceptionTranslatorExecuteListener(
					(Function<ExecuteContext, SQLExceptionTranslator>) null))
			.withMessage("'translatorFactory' must not be null");
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource
	void exceptionTranslatesSqlExceptions(SQLDialect dialect, SQLException sqlException) {
		ExecuteContext context = mockContext(dialect, sqlException);
		this.listener.exception(context);
		then(context).should().exception(assertArg((ex) -> assertThat(ex).isInstanceOf(BadSqlGrammarException.class)));
	}

	@Test
	void exceptionWhenExceptionCannotBeTranslatedDoesNotCallExecuteContextException() {
		ExecuteContext context = mockContext(SQLDialect.POSTGRES, new SQLException(null, null, 123456789));
		this.listener.exception(context);
		then(context).should(never()).exception(any());
	}

	@Test
	void exceptionWhenHasCustomTranslatorFactory() {
		SQLExceptionTranslator translator = (task, sql, ex) -> new BadSqlGrammarException(task, "sql", ex);
		ExceptionTranslatorExecuteListener listener = new DefaultExceptionTranslatorExecuteListener(
				(context) -> translator);
		SQLException sqlException = sqlException(123);
		ExecuteContext context = mockContext(SQLDialect.DUCKDB, sqlException);
		listener.exception(context);
		then(context).should().exception(assertArg((ex) -> assertThat(ex).isInstanceOf(BadSqlGrammarException.class)));
	}

	private ExecuteContext mockContext(SQLDialect dialect, SQLException sqlException) {
		ExecuteContext context = mock(ExecuteContext.class);
		Configuration configuration = mock(Configuration.class);
		given(context.configuration()).willReturn(configuration);
		given(configuration.dialect()).willReturn(dialect);
		given(context.sqlException()).willReturn(sqlException);
		return context;
	}

	static Object[] exceptionTranslatesSqlExceptions() {
		return new Object[] { new Object[] { SQLDialect.DERBY, sqlException("42802") },
				new Object[] { SQLDialect.DERBY, new SQLSyntaxErrorException() },
				new Object[] { SQLDialect.H2, sqlException(42000) },
				new Object[] { SQLDialect.H2, new SQLSyntaxErrorException() },
				new Object[] { SQLDialect.HSQLDB, sqlException(-22) },
				new Object[] { SQLDialect.HSQLDB, new SQLSyntaxErrorException() },
				new Object[] { SQLDialect.MARIADB, sqlException(1054) },
				new Object[] { SQLDialect.MARIADB, new SQLSyntaxErrorException() },
				new Object[] { SQLDialect.MYSQL, sqlException(1054) },
				new Object[] { SQLDialect.MYSQL, new SQLSyntaxErrorException() },
				new Object[] { SQLDialect.POSTGRES, sqlException("03000") },
				new Object[] { SQLDialect.POSTGRES, new SQLSyntaxErrorException() },
				new Object[] { SQLDialect.SQLITE, sqlException("21000") },
				new Object[] { SQLDialect.SQLITE, new SQLSyntaxErrorException() } };
	}

	private static SQLException sqlException(String sqlState) {
		return new SQLException(null, sqlState);
	}

	private static SQLException sqlException(int vendorCode) {
		return new SQLException(null, null, vendorCode);
	}

}
