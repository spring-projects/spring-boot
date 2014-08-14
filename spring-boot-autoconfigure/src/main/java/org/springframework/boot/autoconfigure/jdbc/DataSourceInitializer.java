/*
 * Copyright 2012-2014 the original author or authors.
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.datasource.init.DatabasePopulatorUtils;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.util.StringUtils;

/**
 * Bean to handle {@link DataSource} initialization by running {@literal schema-*.sql} on
 * {@link PostConstruct} and and {@literal data-*.sql} SQL scripts on a
 * {@link DataSourceInitializedEvent}.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @since 1.1.0
 * @see DataSourceAutoConfiguration
 */
class DataSourceInitializer implements ApplicationListener<DataSourceInitializedEvent> {

	private static Log logger = LogFactory.getLog(DataSourceInitializer.class);

	@Autowired
	private ConfigurableApplicationContext applicationContext;

	@Autowired(required = false)
	private DataSource dataSource;

	@Autowired
	private DataSourceProperties properties;

	private boolean initialized = false;

	@PostConstruct
	protected void initialize() {
		if (!this.properties.isInitialize()) {
			logger.debug("Initialization disabled (not running DDL scripts)");
			return;
		}
		if (this.dataSource == null) {
			logger.debug("No DataSource found so not initializing");
			return;
		}
		runSchemaScripts();
	}

	private void runSchemaScripts() {
		List<Resource> scripts = getScripts(this.properties.getSchema(), "schema");
		if (!scripts.isEmpty()) {
			runScripts(scripts);
			try {
				this.applicationContext.publishEvent(new DataSourceInitializedEvent(
						this.dataSource));
			}
			catch (IllegalStateException ex) {
				logger.warn("Could not send event to complete DataSource initialization ("
						+ ex.getMessage() + ")");
			}
		}
	}

	@Override
	public void onApplicationEvent(DataSourceInitializedEvent event) {
		if (!this.properties.isInitialize()) {
			logger.debug("Initialization disabled (not running data scripts)");
			return;
		}
		// NOTE the event can happen more than once and
		// the event datasource is not used here
		if (!this.initialized) {
			runDataScripts();
			this.initialized = true;
		}
	}

	private void runDataScripts() {
		List<Resource> scripts = getScripts(this.properties.getData(), "data");
		runScripts(scripts);
	}

	private List<Resource> getScripts(String locations, String fallback) {
		if (locations == null) {
			String platform = this.properties.getPlatform();
			locations = "classpath*:" + fallback + "-" + platform + ".sql,";
			locations += "classpath*:" + fallback + ".sql";
		}
		return getResources(locations);
	}

	private List<Resource> getResources(String locations) {
		List<Resource> resources = new ArrayList<Resource>();
		for (String location : StringUtils.commaDelimitedListToStringArray(locations)) {
			try {
				for (Resource resource : this.applicationContext.getResources(location)) {
					if (resource.exists()) {
						resources.add(resource);
					}
				}
			}
			catch (IOException ex) {
				throw new IllegalStateException("Unable to load resource from "
						+ location, ex);
			}
		}
		return resources;
	}

	private void runScripts(List<Resource> resources) {
		if (resources.isEmpty()) {
			return;
		}
		ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
		populator.setContinueOnError(this.properties.isContinueOnError());
		populator.setSeparator(this.properties.getSeparator());
		populator.setSqlScriptEncoding(this.properties.getSqlScriptEncoding());
		for (Resource resource : resources) {
			populator.addScript(resource);
		}
		DatabasePopulatorUtils.execute(populator, this.dataSource);
	}

}
