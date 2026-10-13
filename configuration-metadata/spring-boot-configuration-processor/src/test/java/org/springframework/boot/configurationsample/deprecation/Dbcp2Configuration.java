/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.configurationsample.deprecation;

import org.apache.commons.dbcp2.BasicDataSource;

import org.springframework.boot.configurationsample.TestConfigurationProperties;

/**
 * Test configuration for DBCP2 {@link BasicDataSource}.
 *
 * @author Moritz Halbritter
 */
public class Dbcp2Configuration {

	@TestConfigurationProperties("spring.datasource.dbcp2")
	BasicDataSource basicDataSource() {
		return new BasicDataSource();
	}

}
