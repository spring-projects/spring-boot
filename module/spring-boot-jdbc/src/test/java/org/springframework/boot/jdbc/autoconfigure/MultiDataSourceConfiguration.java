/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jdbc.autoconfigure;

import javax.sql.DataSource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for multiple {@link DataSource}.
 *
 * @author Phillip Webb
 * @author Kazuki Shimizu
 */
@Configuration(proxyBeanMethods = false)
class MultiDataSourceConfiguration {

	@Bean
	DataSource test1DataSource() {
		return new TestDataSource("test1", false);
	}

	@Bean
	DataSource test2DataSource() {
		return new TestDataSource("test2", false);
	}

}
