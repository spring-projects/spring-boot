/*
 * Copyright 2012-2013 the original author or authors.
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
import java.util.Arrays;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.datasource.init.DatabasePopulatorUtils;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.util.StringUtils;

/**
 * @author Dave Syer
 */
@Configuration
@EnableConfigurationProperties(DataSourceProperties.class)
public class DataSourceInitialization implements
		ApplicationListener<ContextRefreshedEvent> {

	private static Log logger = LogFactory.getLog(DataSourceAutoConfiguration.class);

	@Autowired(required = false)
	private DataSource dataSource;

	@Autowired
	private ApplicationContext applicationContext;

	@Autowired
	private DataSourceProperties properties;

	private boolean initialized = false;

	@Bean
	public ApplicationListener<DataSourceInitializedEvent> dataSourceInitializedListener() {
		return new DataSourceInitializedListener();
	}

	@Override
	public void onApplicationEvent(ContextRefreshedEvent event) {
		if (this.properties.isDeferDdl()) {
			boolean initialize = this.properties.isInitialize();
			if (!initialize) {
				logger.debug("Initialization disabled (not running DDL scripts)");
				return;
			}
			runSchemaScripts();
		}
	}

	@PostConstruct
	protected void initialize() {
		if (!this.properties.isDeferDdl()) {
			boolean initialize = this.properties.isInitialize();
			if (!initialize) {
				logger.debug("Initialization disabled (not running DDL scripts)");
				return;
			}
			runSchemaScripts();
		}
	}

	private void runSchemaScripts() {
		String schema = this.properties.getSchema();
		if (schema == null) {
			String platform = this.properties.getPlatform();
			schema = "classpath*:schema-" + platform + ".sql,";
			schema += "classpath*:schema.sql";
		}
		if (runScripts(schema)) {
			this.applicationContext.publishEvent(new DataSourceInitializedEvent(
					this.dataSource));
		}
	}

	private void runDataScripts() {
		if (this.initialized) {
			return;
		}
		String schema = this.properties.getData();
		if (schema == null) {
			String platform = this.properties.getPlatform();
			schema = "classpath*:data-" + platform + ".sql,";
			schema += "classpath*:data.sql";
		}
		runScripts(schema);
		this.initialized = true;
	}

	private boolean runScripts(String scripts) {

		if (this.dataSource == null) {
			logger.debug("No DataSource found so not initializing");
			return false;
		}

		List<Resource> resources = getSchemaResources(scripts);

		boolean continueOnError = this.properties.isContinueOnError();
		boolean exists = false;
		ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
		for (Resource resource : resources) {
			if (resource.exists()) {
				exists = true;
				populator.addScript(resource);
				populator.setContinueOnError(continueOnError);
			}
		}
		populator.setSeparator(this.properties.getSeparator());

		if (exists) {
			DatabasePopulatorUtils.execute(populator, this.dataSource);
		}

		return exists;

	}

	private List<Resource> getSchemaResources(String schema) {
		List<Resource> resources = new ArrayList<Resource>();
		for (String schemaLocation : StringUtils.commaDelimitedListToStringArray(schema)) {
			try {
				resources.addAll(Arrays.asList(this.applicationContext
						.getResources(schemaLocation)));
			}
			catch (IOException ex) {
				throw new IllegalStateException("Unable to load resource from "
						+ schemaLocation, ex);
			}
		}
		return resources;
	}

	public static class DataSourceInitializedEvent extends ApplicationEvent {

		public DataSourceInitializedEvent(DataSource source) {
			super(source);
		}

	}

	private class DataSourceInitializedListener implements
			ApplicationListener<DataSourceInitializedEvent> {

		@Override
		public void onApplicationEvent(DataSourceInitializedEvent event) {
			runDataScripts();
		}

	}

}
