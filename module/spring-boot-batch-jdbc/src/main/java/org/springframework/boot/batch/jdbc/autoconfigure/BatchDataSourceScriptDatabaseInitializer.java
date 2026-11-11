/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.batch.jdbc.autoconfigure;

import javax.sql.DataSource;

import org.springframework.boot.jdbc.init.DataSourceScriptDatabaseInitializer;
import org.springframework.boot.jdbc.init.PropertiesBasedDataSourceScriptDatabaseInitializer;

/**
 * {@link DataSourceScriptDatabaseInitializer} for the Spring Batch database. May be
 * registered as a bean to override auto-configuration.
 *
 * @author Dave Syer
 * @author Vedran Pavic
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @author Yanming Zhou
 * @since 4.0.0
 */
public class BatchDataSourceScriptDatabaseInitializer
		extends PropertiesBasedDataSourceScriptDatabaseInitializer<BatchJdbcProperties> {

	/**
	 * Create a new {@link BatchDataSourceScriptDatabaseInitializer} instance.
	 * @param dataSource the Spring Batch data source
	 * @param properties the Spring Batch JDBC properties
	 */
	public BatchDataSourceScriptDatabaseInitializer(DataSource dataSource, BatchJdbcProperties properties) {
		super(dataSource, properties);
	}

}
