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

package org.springframework.boot.context.properties.migrator;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.configurationmetadata.ConfigurationMetadataRepository;
import org.springframework.boot.configurationmetadata.ConfigurationMetadataRepositoryJsonBuilder;
import org.springframework.boot.context.event.ApplicationFailedEvent;
import org.springframework.boot.context.event.ApplicationPreparedEvent;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.event.SpringApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

/**
 * An {@link ApplicationListener} that inspects the {@link ConfigurableEnvironment
 * environment} for configuration keys that need to be migrated. Automatically renames the
 * keys that have a matching replacement and logs a report of what was discovered.
 *
 * @author Stephane Nicoll
 */
class PropertiesMigrationListener implements ApplicationListener<SpringApplicationEvent> {

	private static final Log logger = LogFactory.getLog(PropertiesMigrationListener.class);

	private PropertiesMigrationReport report;

	private boolean reported;

	/**
     * This method is called when an application event is triggered.
     * It checks the type of the event and performs specific actions accordingly.
     * If the event is an instance of ApplicationPreparedEvent, it calls the onApplicationPreparedEvent method.
     * If the event is an instance of ApplicationReadyEvent or ApplicationFailedEvent, it logs a legacy properties report.
     *
     * @param event the application event that is triggered
     */
    @Override
	public void onApplicationEvent(SpringApplicationEvent event) {
		if (event instanceof ApplicationPreparedEvent preparedEvent) {
			onApplicationPreparedEvent(preparedEvent);
		}
		if (event instanceof ApplicationReadyEvent || event instanceof ApplicationFailedEvent) {
			logLegacyPropertiesReport();
		}
	}

	/**
     * This method is called when the application is prepared.
     * It loads the configuration metadata repository and creates a PropertiesMigrationReporter object.
     * The reporter is used to generate a report of properties migration.
     * 
     * @param event The ApplicationPreparedEvent object representing the event.
     *              It contains the application context and environment information.
     */
    private void onApplicationPreparedEvent(ApplicationPreparedEvent event) {
		ConfigurationMetadataRepository repository = loadRepository();
		PropertiesMigrationReporter reporter = new PropertiesMigrationReporter(repository,
				event.getApplicationContext().getEnvironment());
		this.report = reporter.getReport();
	}

	/**
     * Loads the configuration metadata repository.
     * 
     * @return the loaded configuration metadata repository
     * @throws IllegalStateException if failed to load metadata
     */
    private ConfigurationMetadataRepository loadRepository() {
		try {
			return loadRepository(ConfigurationMetadataRepositoryJsonBuilder.create());
		}
		catch (IOException ex) {
			throw new IllegalStateException("Failed to load metadata", ex);
		}
	}

	/**
     * Loads the configuration metadata repository using the provided builder.
     * 
     * @param builder the ConfigurationMetadataRepositoryJsonBuilder used to build the repository
     * @return the loaded ConfigurationMetadataRepository
     * @throws IOException if an I/O error occurs while loading the repository
     */
    private ConfigurationMetadataRepository loadRepository(ConfigurationMetadataRepositoryJsonBuilder builder)
			throws IOException {
		Resource[] resources = new PathMatchingResourcePatternResolver()
			.getResources("classpath*:/META-INF/spring-configuration-metadata.json");
		for (Resource resource : resources) {
			try (InputStream inputStream = resource.getInputStream()) {
				builder.withJsonResource(inputStream);
			}
		}
		return builder.build();
	}

	/**
     * Logs the legacy properties report.
     * 
     * This method logs the warning and error reports from the legacy properties report.
     * If the report is null or has already been reported, the method returns without doing anything.
     * 
     * @see PropertiesMigrationListener
     */
    private void logLegacyPropertiesReport() {
		if (this.report == null || this.reported) {
			return;
		}
		String warningReport = this.report.getWarningReport();
		if (warningReport != null) {
			logger.warn(warningReport);
		}
		String errorReport = this.report.getErrorReport();
		if (errorReport != null) {
			logger.error(errorReport);
		}
		this.reported = true;
	}

}
