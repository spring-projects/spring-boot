/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.integration.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.init.DatabaseInitializationProperties;

/**
 * Configuration properties for Spring Integration JDBC.
 *
 * @author Vedran Pavic
 * @author Stephane Nicoll
 * @author Artem Bilan
 * @author Yanming Zhou
 * @since 4.0.0
 */
@ConfigurationProperties("spring.integration.jdbc")
public class IntegrationJdbcProperties extends DatabaseInitializationProperties {

	private static final String DEFAULT_SCHEMA_LOCATION = "classpath:org/springframework/"
			+ "integration/jdbc/schema-@@platform@@.sql";

	@Override
	public String getDefaultSchemaLocation() {
		return DEFAULT_SCHEMA_LOCATION;
	}

}
