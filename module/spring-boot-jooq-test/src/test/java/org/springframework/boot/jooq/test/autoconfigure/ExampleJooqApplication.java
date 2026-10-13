/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jooq.test.autoconfigure;

import javax.sql.DataSource;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

/**
 * Example {@link SpringBootApplication @SpringBootApplication} used with
 * {@link JooqTest @JooqTest} tests.
 *
 * @author Michael Simons
 */
@SpringBootApplication
public class ExampleJooqApplication {

	@Bean
	public DataSource dataSource() {
		return new EmbeddedDatabaseBuilder().generateUniqueName(true).setType(EmbeddedDatabaseType.HSQL).build();
	}

}
