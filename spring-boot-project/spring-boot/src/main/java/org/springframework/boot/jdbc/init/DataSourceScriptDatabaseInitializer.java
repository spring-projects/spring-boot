/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.jdbc.init;

import java.nio.charset.Charset;
import java.util.List;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
import org.springframework.boot.sql.init.AbstractScriptDatabaseInitializer;
import org.springframework.boot.sql.init.DatabaseInitializationSettings;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.datasource.init.DatabasePopulatorUtils;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

/**
 * {@link InitializingBean} that performs {@link DataSource} initialization using schema
 * (DDL) and data (DML) scripts.
 *
 * @author Andy Wilkinson
 * @since 2.5.0
 */
public class DataSourceScriptDatabaseInitializer extends AbstractScriptDatabaseInitializer {

	private static final Log logger = LogFactory.getLog(DataSourceScriptDatabaseInitializer.class);

	private final DataSource dataSource;

	/**
	 * Creates a new {@link DataSourceScriptDatabaseInitializer} that will initialize the
	 * given {@code DataSource} using the given settings.
	 * @param dataSource data source to initialize
	 * @param settings initialization settings
	 */
	public DataSourceScriptDatabaseInitializer(DataSource dataSource, DatabaseInitializationSettings settings) {
		super(settings);
		this.dataSource = dataSource;
	}

	/**
	 * Returns the {@code DataSource} that will be initialized.
	 * @return the initialization data source
	 */
	protected final DataSource getDataSource() {
		return this.dataSource;
	}

	@Override
	protected boolean isEmbeddedDatabase() {
		try {
			return EmbeddedDatabaseConnection.isEmbedded(this.dataSource);
		}
		catch (Exception ex) {
			logger.debug("Could not determine if datasource is embedded", ex);
			return false;
		}
	}

	@Override
	protected void runScripts(List<Resource> resources, boolean continueOnError, String separator, Charset encoding) {
		ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
		populator.setContinueOnError(continueOnError);
		populator.setSeparator(separator);
		if (encoding != null) {
			populator.setSqlScriptEncoding(encoding.name());
		}
		for (Resource resource : resources) {
			populator.addScript(resource);
		}
		DatabasePopulatorUtils.execute(populator, this.dataSource);
	}

}
