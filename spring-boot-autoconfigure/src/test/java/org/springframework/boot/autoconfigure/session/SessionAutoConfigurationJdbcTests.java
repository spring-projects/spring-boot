/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.autoconfigure.session;

import java.util.Arrays;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.EmbeddedDataSourceConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.session.jdbc.JdbcOperationsSessionRepository;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JDBC specific tests for {@link SessionAutoConfiguration}.
 *
 * @author Vedran Pavic
 * @author Stephane Nicoll
 */
public class SessionAutoConfigurationJdbcTests
		extends AbstractSessionAutoConfigurationTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void defaultConfig() {
		load(Arrays.asList(EmbeddedDataSourceConfiguration.class,
				JdbcTemplateAutoConfiguration.class,
				DataSourceTransactionManagerAutoConfiguration.class),
				"spring.session.store-type=jdbc");
		JdbcOperationsSessionRepository repository = validateSessionRepository(
				JdbcOperationsSessionRepository.class);
		assertThat(new DirectFieldAccessor(repository).getPropertyValue("tableName"))
				.isEqualTo("SPRING_SESSION");
		assertThat(this.context.getBean(SessionProperties.class).getJdbc()
				.getInitializer().isEnabled()).isTrue();
		assertThat(this.context.getBean(JdbcOperations.class)
				.queryForList("select * from SPRING_SESSION")).isEmpty();
	}

	@Test
	public void disableDatabaseInitializer() {
		load(Arrays.asList(EmbeddedDataSourceConfiguration.class,
				DataSourceTransactionManagerAutoConfiguration.class),
				"spring.session.store-type=jdbc",
				"spring.session.jdbc.initializer.enabled=false");
		JdbcOperationsSessionRepository repository = validateSessionRepository(
				JdbcOperationsSessionRepository.class);
		assertThat(new DirectFieldAccessor(repository).getPropertyValue("tableName"))
				.isEqualTo("SPRING_SESSION");
		assertThat(this.context.getBean(SessionProperties.class).getJdbc()
				.getInitializer().isEnabled()).isFalse();
		this.thrown.expect(BadSqlGrammarException.class);
		assertThat(this.context.getBean(JdbcOperations.class)
				.queryForList("select * from SPRING_SESSION")).isEmpty();
	}

	@Test
	public void customTableName() {
		load(Arrays.asList(EmbeddedDataSourceConfiguration.class,
				DataSourceTransactionManagerAutoConfiguration.class),
				"spring.session.store-type=jdbc",
				"spring.session.jdbc.table-name=FOO_BAR",
				"spring.session.jdbc.schema=classpath:session/custom-schema-h2.sql");
		JdbcOperationsSessionRepository repository = validateSessionRepository(
				JdbcOperationsSessionRepository.class);
		assertThat(new DirectFieldAccessor(repository).getPropertyValue("tableName"))
				.isEqualTo("FOO_BAR");
		assertThat(this.context.getBean(SessionProperties.class).getJdbc()
				.getInitializer().isEnabled()).isTrue();
		assertThat(this.context.getBean(JdbcOperations.class)
				.queryForList("select * from FOO_BAR")).isEmpty();
	}

	@Test
	public void customTableNameWithDefaultSchemaDisablesInitializer() {
		load(Arrays.asList(EmbeddedDataSourceConfiguration.class,
				DataSourceTransactionManagerAutoConfiguration.class),
				"spring.session.store-type=jdbc",
				"spring.session.jdbc.table-name=FOO_BAR");
		JdbcOperationsSessionRepository repository = validateSessionRepository(
				JdbcOperationsSessionRepository.class);
		assertThat(new DirectFieldAccessor(repository).getPropertyValue("tableName"))
				.isEqualTo("FOO_BAR");
		assertThat(this.context.getBean(SessionProperties.class).getJdbc()
				.getInitializer().isEnabled()).isFalse();
		this.thrown.expect(BadSqlGrammarException.class);
		assertThat(this.context.getBean(JdbcOperations.class)
				.queryForList("select * from SPRING_SESSION")).isEmpty();
	}

}
