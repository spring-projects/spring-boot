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

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternUtils;
import org.springframework.jdbc.datasource.init.DatabasePopulatorUtils;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.util.CollectionUtils;

/**
 * {@link InitializingBean} that performs {@link DataSource} initialization using schema
 * (DDL) and data (DML) scripts.
 *
 * @author Andy Wilkinson
 * @since 2.5.0
 */
public class ScriptDataSourceInitializer implements ResourceLoaderAware, InitializingBean {

	private static final String OPTIONAL_LOCATION_PREFIX = "optional:";

	private final DataSource dataSource;

	private final DataSourceInitializationSettings settings;

	private volatile ResourceLoader resourceLoader;

	/**
	 * Creates a new {@link ScriptDataSourceInitializer} that will initialize the given
	 * {@code DataSource} using the given settings.
	 * @param dataSource data source to initialize
	 * @param settings initialization settings
	 */
	public ScriptDataSourceInitializer(DataSource dataSource, DataSourceInitializationSettings settings) {
		this.dataSource = dataSource;
		this.settings = settings;
	}

	/**
	 * Returns the {@code DataSource} that will be initialized.
	 * @return the initialization data source
	 */
	protected final DataSource getDataSource() {
		return this.dataSource;
	}

	@Override
	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		initializeDatabase();
	}

	/**
	 * Initializes the database by applying schema and data scripts.
	 * @return {@code true} if one or more scripts were applied to the database, otherwise
	 * {@code false}
	 */
	public boolean initializeDatabase() {
		ScriptLocationResolver locationResolver = new ScriptLocationResolver(this.resourceLoader);
		boolean initialized = applySchemaScripts(locationResolver);
		initialized = applyDataScripts(locationResolver) || initialized;
		return initialized;
	}

	private boolean applySchemaScripts(ScriptLocationResolver locationResolver) {
		return applyScripts(this.settings.getSchemaLocations(), "schema", locationResolver);
	}

	private boolean applyDataScripts(ScriptLocationResolver locationResolver) {
		return applyScripts(this.settings.getDataLocations(), "data", locationResolver);
	}

	private boolean applyScripts(List<String> locations, String type, ScriptLocationResolver locationResolver) {
		List<Resource> scripts = getScripts(locations, type, locationResolver);
		if (!scripts.isEmpty()) {
			runScripts(scripts);
		}
		return !scripts.isEmpty();
	}

	private List<Resource> getScripts(List<String> locations, String type, ScriptLocationResolver locationResolver) {
		if (CollectionUtils.isEmpty(locations)) {
			return Collections.emptyList();
		}
		List<Resource> resources = new ArrayList<>();
		for (String location : locations) {
			boolean optional = location.startsWith(OPTIONAL_LOCATION_PREFIX);
			if (optional) {
				location = location.substring(OPTIONAL_LOCATION_PREFIX.length());
			}
			for (Resource resource : doGetResources(location, locationResolver)) {
				if (resource.exists()) {
					resources.add(resource);
				}
				else if (!optional) {
					throw new IllegalStateException("No " + type + " scripts found at location '" + location + "'");
				}
			}
		}
		return resources;
	}

	private List<Resource> doGetResources(String location, ScriptLocationResolver locationResolver) {
		try {
			return locationResolver.resolve(location);
		}
		catch (Exception ex) {
			throw new IllegalStateException("Unable to load resources from " + location, ex);
		}
	}

	private void runScripts(List<Resource> resources) {
		if (resources.isEmpty()) {
			return;
		}
		runScripts(resources, this.settings.isContinueOnError(), this.settings.getSeparator(),
				this.settings.getEncoding());
	}

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

	private static class ScriptLocationResolver {

		private final ResourcePatternResolver resourcePatternResolver;

		ScriptLocationResolver(ResourceLoader resourceLoader) {
			this.resourcePatternResolver = ResourcePatternUtils.getResourcePatternResolver(resourceLoader);
		}

		private List<Resource> resolve(String location) throws IOException {
			List<Resource> resources = new ArrayList<>(
					Arrays.asList(this.resourcePatternResolver.getResources(location)));
			resources.sort((r1, r2) -> {
				try {
					return r1.getURL().toString().compareTo(r2.getURL().toString());
				}
				catch (IOException ex) {
					return 0;
				}
			});
			return resources;
		}

	}

}
