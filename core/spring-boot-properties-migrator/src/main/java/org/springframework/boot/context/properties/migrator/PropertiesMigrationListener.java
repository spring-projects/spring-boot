/*
 * Copyright 2012-present the original author or authors.
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
import java.util.Locale;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

import org.springframework.boot.configurationmetadata.ConfigurationMetadataRepository;
import org.springframework.boot.configurationmetadata.ConfigurationMetadataRepositoryJsonBuilder;
import org.springframework.boot.context.event.ApplicationFailedEvent;
import org.springframework.boot.context.event.ApplicationPreparedEvent;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.event.SpringApplicationEvent;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

/**
 * An {@link ApplicationListener} that inspects the {@link ConfigurableEnvironment
 * environment} for configuration keys that need to be migrated. Automatically renames the
 * keys that have a matching replacement and logs a report of what was discovered. Can
 * optionally fail application startup when such keys are found.
 *
 * @author Stephane Nicoll
 * @author Hyunwoo Jung
 */
class PropertiesMigrationListener implements ApplicationListener<SpringApplicationEvent> {

	private static final Log logger = LogFactory.getLog(PropertiesMigrationListener.class);

	private static final String FAIL_PROPERTY = "spring.tools.properties-migrator.fail";

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
		ConfigurableEnvironment environment = event.getApplicationContext().getEnvironment();
		PropertiesMigrationReport report = new PropertiesMigrationReporter(loadRepository(), environment).getReport();
		this.report = report;
		Fail fail = Binder.get(environment).bind(FAIL_PROPERTY, Fail.class).orElse(Fail.NEVER);
		failIfNecessary(report, fail);
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

	private void failIfNecessary(PropertiesMigrationReport report, Fail fail) {
		if (shouldFail(report, fail)) {
			throw new IllegalStateException("Found configuration keys that need to be migrated (%s=%s)"
				.formatted(FAIL_PROPERTY, fail.name().toLowerCase(Locale.ROOT).replace('_', '-')));
		}
	}

	private static boolean shouldFail(PropertiesMigrationReport report, Fail fail) {
		return switch (fail) {
			case NEVER -> false;
			case ON_ERROR -> report.getErrorReport() != null;
			case ON_WARNING -> report.getErrorReport() != null || report.getWarningReport() != null;
		};
	}

	/**
	 * Conditions under which property migration should cause application startup to fail.
	 */
	enum Fail {

		/**
		 * Do not fail application startup due to migration warnings or errors.
		 */
		NEVER,

		/**
		 * Fail application startup if the migration report contains errors.
		 */
		ON_ERROR,

		/**
		 * Fail application startup if the migration report contains warnings or errors.
		 */
		ON_WARNING

	}

}
