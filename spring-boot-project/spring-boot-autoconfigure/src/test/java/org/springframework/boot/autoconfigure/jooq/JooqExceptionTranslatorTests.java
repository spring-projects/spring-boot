/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.autoconfigure.jooq;

import java.sql.SQLException;

import org.jooq.Configuration;
import org.jooq.ExecuteContext;
import org.jooq.SQLDialect;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;

import org.springframework.jdbc.BadSqlGrammarException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

/**
 * Tests for {@link JooqExceptionTranslator}
 *
 * @author Andy Wilkinson
 */
class JooqExceptionTranslatorTests {

	private final JooqExceptionTranslator exceptionTranslator = new JooqExceptionTranslator();

	@ParameterizedTest(name = "{0}")
	@MethodSource
	void exceptionTranslation(SQLDialect dialect, SQLException sqlException) {
		ExecuteContext context = mock(ExecuteContext.class);
		Configuration configuration = mock(Configuration.class);
		given(context.configuration()).willReturn(configuration);
		given(configuration.dialect()).willReturn(dialect);
		given(context.sqlException()).willReturn(sqlException);
		this.exceptionTranslator.exception(context);
		ArgumentCaptor<RuntimeException> captor = ArgumentCaptor.forClass(RuntimeException.class);
		then(context).should().exception(captor.capture());
		assertThat(captor.getValue()).isInstanceOf(BadSqlGrammarException.class);
	}

	@Test
	void whenExceptionCannotBeTranslatedThenExecuteContextExceptionIsNotCalled() {
		ExecuteContext context = mock(ExecuteContext.class);
		Configuration configuration = mock(Configuration.class);
		given(context.configuration()).willReturn(configuration);
		given(configuration.dialect()).willReturn(SQLDialect.POSTGRES);
		given(context.sqlException()).willReturn(new SQLException(null, null, 123456789));
		this.exceptionTranslator.exception(context);
		then(context).should(never()).exception(any());
	}

	static Object[] exceptionTranslation() {
		return new Object[] { new Object[] { SQLDialect.DERBY, sqlException("42802") },
				new Object[] { SQLDialect.H2, sqlException(42000) },
				new Object[] { SQLDialect.HSQLDB, sqlException(-22) },
				new Object[] { SQLDialect.MARIADB, sqlException(1054) },
				new Object[] { SQLDialect.MYSQL, sqlException(1054) },
				new Object[] { SQLDialect.POSTGRES, sqlException("03000") },
				new Object[] { SQLDialect.SQLITE, sqlException("21000") } };
	}

	private static SQLException sqlException(String sqlState) {
		return new SQLException(null, sqlState);
	}

	private static SQLException sqlException(int vendorCode) {
		return new SQLException(null, null, vendorCode);
	}

}
