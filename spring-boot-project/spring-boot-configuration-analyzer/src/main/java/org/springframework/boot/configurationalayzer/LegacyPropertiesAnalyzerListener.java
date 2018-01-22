/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.configurationalayzer;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

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
 * environment} for legacy configuration keys. Automatically renames the keys that
 * have a matching replacement and log a report of what  was discovered.
 *
 * @author Stephane Nicoll
 * @since 2.0.0
 */
public class LegacyPropertiesAnalyzerListener
		implements ApplicationListener<SpringApplicationEvent> {

	private static final Log logger = LogFactory.getLog(LegacyPropertiesAnalyzerListener.class);

	private LegacyPropertiesAnalysis analysis;

	private boolean analysisLogged;

	@Override
	public void onApplicationEvent(SpringApplicationEvent event) {
		if (event instanceof ApplicationPreparedEvent) {
			onApplicationPreparedEvent((ApplicationPreparedEvent) event);
		}
		if (event instanceof ApplicationReadyEvent
				|| event instanceof ApplicationFailedEvent) {
			logLegacyPropertiesAnalysis();
		}
	}

	private void onApplicationPreparedEvent(ApplicationPreparedEvent event) {
		ConfigurationMetadataRepository repository = loadRepository();
		ConfigurableEnvironment environment =
				event.getApplicationContext().getEnvironment();
		LegacyPropertiesAnalyzer validator = new LegacyPropertiesAnalyzer(
				repository, environment);
		this.analysis = validator.analyseLegacyProperties();
	}

	private void logLegacyPropertiesAnalysis() {
		if (this.analysis == null || this.analysisLogged) {
			return;
		}
		String warningReport = this.analysis.createWarningReport();
		String errorReport = this.analysis.createErrorReport();
		if (warningReport != null) {
			logger.warn(warningReport);
		}
		if (errorReport != null) {
			logger.error(errorReport);
		}
		this.analysisLogged = true;
	}

	private ConfigurationMetadataRepository loadRepository() {
		try {
			ConfigurationMetadataRepositoryJsonBuilder builder = ConfigurationMetadataRepositoryJsonBuilder.create();
			for (InputStream inputStream : getResources()) {
				builder.withJsonResource(inputStream);
			}
			return builder.build();
		}
		catch (IOException ex) {
			throw new IllegalStateException("Failed to load metadata", ex);
		}
	}

	private List<InputStream> getResources() throws IOException {
		Resource[] resources = new PathMatchingResourcePatternResolver()
				.getResources("classpath*:/META-INF/spring-configuration-metadata.json");
		List<InputStream> result = new ArrayList<>();
		for (Resource resource : resources) {
			result.add(resource.getInputStream());
		}
		return result;
	}

}
