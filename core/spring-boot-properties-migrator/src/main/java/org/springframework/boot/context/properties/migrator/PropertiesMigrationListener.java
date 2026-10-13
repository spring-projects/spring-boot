/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.context.properties.migrator;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

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

	private @Nullable PropertiesMigrationReport report;

	private boolean reported;

	@Override
	public void onApplicationEvent(SpringApplicationEvent event) {
		if (event instanceof ApplicationPreparedEvent preparedEvent) {
			onApplicationPreparedEvent(preparedEvent);
		}
		if (event instanceof ApplicationReadyEvent || event instanceof ApplicationFailedEvent) {
			logLegacyPropertiesReport();
		}
	}

	private void onApplicationPreparedEvent(ApplicationPreparedEvent event) {
		ConfigurationMetadataRepository repository = loadRepository();
		PropertiesMigrationReporter reporter = new PropertiesMigrationReporter(repository,
				event.getApplicationContext().getEnvironment());
		this.report = reporter.getReport();
	}

	private ConfigurationMetadataRepository loadRepository() {
		try {
			return loadRepository(ConfigurationMetadataRepositoryJsonBuilder.create());
		}
		catch (IOException ex) {
			throw new IllegalStateException("Failed to load metadata", ex);
		}
	}

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
