/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.autoconfigure.jdbc;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Test;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link EmbeddedDataSourceConfiguration}.
 *
 * @author Dave Syer
 * @author Stephane Nicoll
 */
public class EmbeddedDataSourceConfigurationTests {

	private AnnotationConfigApplicationContext context;

	@After
	public void closeContext() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void defaultEmbeddedDatabase() {
		this.context = load();
		assertThat(this.context.getBean(DataSource.class)).isNotNull();
	}

	@Test
	public void generateUniqueName() throws Exception {
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
