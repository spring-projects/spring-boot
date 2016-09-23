/*
 * Copyright 2012-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
import java.util.Properties;
import javax.sql.DataSource;

import org.junit.Test;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.PropertiesPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link EmbeddedDataSourceConfiguration}.
 *
 * @author Dave Syer
 */
public class EmbeddedDataSourceConfigurationTests {

	private AnnotationConfigApplicationContext context;

	@Test
	public void testDefaultEmbeddedDatabase() throws Exception {
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(EmbeddedDataSourceConfiguration.class);
		this.context.refresh();
		assertThat(this.context.getBean(DataSource.class)).isNotNull();
		this.context.close();
	}

	@Test
	public void generatesUniqueDatabaseName() throws Exception {
		Properties myProps = new Properties();
		myProps.setProperty("spring.datasource.generate-name", "true");

		this.context = new AnnotationConfigApplicationContext();
		this.context.register(EmbeddedDataSourceConfiguration.class);
		this.context.getEnvironment().getPropertySources().addFirst(new PropertiesPropertySource("whatever", myProps));
		this.context.refresh();
		DataSource dataSource = this.context.getBean(DataSource.class);
		assertThat(getDatabaseName(dataSource)).isNotEqualToIgnoringCase("testdb");
		this.context.close();
	}

	private String getDatabaseName(DataSource dataSource) throws SQLException {
		Connection connection = dataSource.getConnection();
		try {
			ResultSet catalogs = connection.getMetaData().getCatalogs();
			catalogs.next();
			return catalogs.getString(1);
		} finally {
			connection.close();
		}
	}

}
