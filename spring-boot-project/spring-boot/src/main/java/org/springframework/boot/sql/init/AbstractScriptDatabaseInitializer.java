/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.sql.init;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternUtils;
import org.springframework.util.CollectionUtils;

/**
 * Base class for an {@link InitializingBean} that performs SQL database initialization
 * using schema (DDL) and data (DML) scripts.
 *
 * @author Andy Wilkinson
 * @since 2.5.0
 */
public abstract class AbstractScriptDatabaseInitializer implements ResourceLoaderAware, InitializingBean {

	private static final String OPTIONAL_LOCATION_PREFIX = "optional:";

	private final DatabaseInitializationSettings settings;

	private volatile ResourceLoader resourceLoader;

	/**
	 * Creates a new {@link AbstractScriptDatabaseInitializer} that will initialize the
	 * database using the given settings.
	 * @param settings initialization settings
	 */
	protected AbstractScriptDatabaseInitializer(DatabaseInitializationSettings settings) {
		this.settings = settings;
	}

	/**
     * Sets the resource loader for this script database initializer.
     * 
     * @param resourceLoader the resource loader to be set
     */
    @Override
	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}

	/**
     * Initializes the database after all properties have been set.
     * 
     * @throws Exception if an error occurs during the initialization process
     */
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
		return applyDataScripts(locationResolver) || initialized;
	}

	/**
     * Returns a boolean value indicating whether the database initialization is enabled.
     * 
     * @return {@code true} if the database initialization is enabled, {@code false} otherwise
     */
    private boolean isEnabled() {
		if (this.settings.getMode() == DatabaseInitializationMode.NEVER) {
			return false;
		}
		return this.settings.getMode() == DatabaseInitializationMode.ALWAYS || isEmbeddedDatabase();
	}

	/**
	 * Returns whether the database that is to be initialized is embedded.
	 * @return {@code true} if the database is embedded, otherwise {@code false}
	 * @since 2.5.1
	 */
	protected boolean isEmbeddedDatabase() {
		throw new IllegalStateException(
				"Database initialization mode is '" + this.settings.getMode() + "' and database type is unknown");
	}

	/**
     * Applies the schema scripts using the provided location resolver.
     * 
     * @param locationResolver the script location resolver
     * @return true if the scripts were successfully applied, false otherwise
     */
    private boolean applySchemaScripts(ScriptLocationResolver locationResolver) {
		return applyScripts(this.settings.getSchemaLocations(), "schema", locationResolver);
	}

	/**
     * Applies data scripts to the database using the provided location resolver.
     * 
     * @param locationResolver the script location resolver to use
     * @return true if the data scripts were successfully applied, false otherwise
     */
    private boolean applyDataScripts(ScriptLocationResolver locationResolver) {
		return applyScripts(this.settings.getDataLocations(), "data", locationResolver);
	}

	/**
     * Applies the scripts to the database.
     * 
     * @param locations         the list of script locations
     * @param type              the type of scripts to apply
     * @param locationResolver  the script location resolver
     * @return                  true if the scripts were applied successfully, false otherwise
     */
    private boolean applyScripts(List<String> locations, String type, ScriptLocationResolver locationResolver) {
		List<Resource> scripts = getScripts(locations, type, locationResolver);
		if (!scripts.isEmpty() && isEnabled()) {
			runScripts(scripts);
			return true;
		}
		return false;
	}

	/**
     * Retrieves the scripts from the given locations based on the provided type and location resolver.
     * 
     * @param locations         the list of locations where the scripts are located
     * @param type              the type of scripts to retrieve
     * @param locationResolver  the resolver used to resolve the script locations
     * @return                  the list of resources representing the scripts
     * @throws IllegalStateException if no scripts are found at a non-optional location
     */
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
				if (resource.isReadable()) {
					resources.add(resource);
				}
				else if (!optional) {
					throw new IllegalStateException("No " + type + " scripts found at location '" + location + "'");
				}
			}
		}
		return resources;
	}

	/**
     * Retrieves a list of resources from the specified location using the provided location resolver.
     * 
     * @param location the location of the resources to retrieve
     * @param locationResolver the resolver used to resolve the location
     * @return a list of resources retrieved from the specified location
     * @throws IllegalStateException if unable to load resources from the specified location
     */
    private List<Resource> doGetResources(String location, ScriptLocationResolver locationResolver) {
		try {
			return locationResolver.resolve(location);
		}
		catch (Exception ex) {
			throw new IllegalStateException("Unable to load resources from " + location, ex);
		}
	}

	/**
     * Runs the scripts using the provided list of resources.
     * 
     * @param resources the list of resources containing the scripts to be executed
     */
    private void runScripts(List<Resource> resources) {
		runScripts(new Scripts(resources).continueOnError(this.settings.isContinueOnError())
			.separator(this.settings.getSeparator())
			.encoding(this.settings.getEncoding()));
	}

	/**
	 * Initialize the database by running the given {@code scripts}.
	 * @param scripts the scripts to run
	 * @since 3.0.0
	 */
	protected abstract void runScripts(Scripts scripts);

	/**
     * ScriptLocationResolver class.
     */
    private static class ScriptLocationResolver {

		private final ResourcePatternResolver resourcePatternResolver;

		/**
         * Constructs a new ScriptLocationResolver with the specified ResourceLoader.
         * 
         * @param resourceLoader the ResourceLoader to be used for resolving resources
         */
        ScriptLocationResolver(ResourceLoader resourceLoader) {
			this.resourcePatternResolver = ResourcePatternUtils.getResourcePatternResolver(resourceLoader);
		}

		/**
         * Resolves the resources at the specified location.
         * 
         * @param location the location of the resources to resolve
         * @return a list of resolved resources
         * @throws IOException if an I/O error occurs while resolving the resources
         */
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

	/**
	 * Scripts to be used to initialize the database.
	 *
	 * @since 3.0.0
	 */
	public static class Scripts implements Iterable<Resource> {

		private final List<Resource> resources;

		private boolean continueOnError = false;

		private String separator = ";";

		private Charset encoding;

		/**
         * Constructs a new instance of the Scripts class with the specified list of resources.
         * 
         * @param resources the list of resources to be assigned to the Scripts instance
         */
        public Scripts(List<Resource> resources) {
			this.resources = resources;
		}

		/**
         * Returns an iterator over the resources in this Scripts object.
         *
         * @return an iterator over the resources in this Scripts object
         */
        @Override
		public Iterator<Resource> iterator() {
			return this.resources.iterator();
		}

		/**
         * Sets whether the script should continue executing even if an error occurs.
         * 
         * @param continueOnError true if the script should continue on error, false otherwise
         * @return the Scripts object with the updated continueOnError value
         */
        public Scripts continueOnError(boolean continueOnError) {
			this.continueOnError = continueOnError;
			return this;
		}

		/**
         * Returns a boolean value indicating whether the program should continue executing
         * even if an error occurs.
         *
         * @return true if the program should continue executing on error, false otherwise
         */
        public boolean isContinueOnError() {
			return this.continueOnError;
		}

		/**
         * Sets the separator for the Scripts object.
         * 
         * @param separator the separator to be set
         * @return the Scripts object with the separator set
         */
        public Scripts separator(String separator) {
			this.separator = separator;
			return this;
		}

		/**
         * Returns the separator used in the Scripts class.
         * 
         * @return the separator used in the Scripts class
         */
        public String getSeparator() {
			return this.separator;
		}

		/**
         * Sets the encoding for the scripts.
         * 
         * @param encoding the charset encoding to be used
         * @return the Scripts object with the updated encoding
         */
        public Scripts encoding(Charset encoding) {
			this.encoding = encoding;
			return this;
		}

		/**
         * Returns the encoding used by the Scripts class.
         * 
         * @return the encoding used by the Scripts class
         */
        public Charset getEncoding() {
			return this.encoding;
		}

	}

}
