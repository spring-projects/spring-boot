/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.diagnostics.analyzer;

import org.springframework.boot.context.properties.InvalidConfigurationPropertiesException;
import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;

/**
 * An {@link AbstractFailureAnalyzer} that performs analysis of failures caused by
 * {@link InvalidConfigurationPropertiesException}.
 *
 * @author Madhura Bhave
 */
public class InvalidConfigurationPropertiesFailureAnalyzer
		extends AbstractFailureAnalyzer<InvalidConfigurationPropertiesException> {

	@Override
	protected FailureAnalysis analyze(Throwable rootFailure, InvalidConfigurationPropertiesException cause) {
		String configurationProperties = cause.getConfigurationProperties().getName();
		String component = cause.getComponent().getSimpleName();
		return new FailureAnalysis(getDescription(configurationProperties, component),
				getAction(configurationProperties, component), cause);
	}

	private String getDescription(String configurationProperties, String component) {
		return configurationProperties + " is annotated with @ConfigurationProperties and @" + component
				+ ". This may cause the @ConfigurationProperties bean to be registered twice.";
	}

	private String getAction(String configurationProperties, String component) {
		return "Remove @" + component + " from " + configurationProperties
				+ " or consider disabling automatic @ConfigurationProperties scanning.";
	}

}
