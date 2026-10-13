/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.howto.dataaccess;

import javax.sql.DataSource;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;

/**
 * A sample {@link SpringBootConfiguration @ConfigurationProperties} that only enables the
 * auto-configuration for the {@link DataSource}.
 *
 * @author Stephane Nicoll
 */
@SpringBootConfiguration
@ImportAutoConfiguration(DataSourceAutoConfiguration.class)
class SampleApp {

}
