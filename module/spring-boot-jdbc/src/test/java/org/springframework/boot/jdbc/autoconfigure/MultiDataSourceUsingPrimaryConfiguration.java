/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jdbc.autoconfigure;

import javax.sql.DataSource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Configuration for multiple {@link DataSource} (one being {@code @Primary}).
 *
 * @author Phillip Webb
 * @author Kazuki Shimizu
 */
@Configuration(proxyBeanMethods = false)
class MultiDataSourceUsingPrimaryConfiguration {

	@Bean
	@Primary
	DataSource test1DataSource() {
		return new TestDataSource("test1", false);
	}

	@Bean
	DataSource test2DataSource() {
		return new TestDataSource("test2", false);
	}

}
