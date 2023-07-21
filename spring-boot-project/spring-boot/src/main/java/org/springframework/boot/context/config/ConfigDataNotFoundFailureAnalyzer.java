/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.context.config;

import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.boot.origin.Origin;

/**
 * An implementation of {@link AbstractFailureAnalyzer} to analyze failures caused by
 * {@link ConfigDataNotFoundException}.
 *
 * @author Michal Mlak
 * @author Phillip Webb
 */
class ConfigDataNotFoundFailureAnalyzer extends AbstractFailureAnalyzer<ConfigDataNotFoundException> {

	@Override
	protected FailureAnalysis analyze(Throwable rootFailure, ConfigDataNotFoundException cause) {
		ConfigDataLocation location = getLocation(cause);
		Origin origin = Origin.from(location);
		String message = String.format("Config data %s does not exist", cause.getReferenceDescription());
		StringBuilder action = new StringBuilder("Check that the value ");
		if (location != null) {
			action.append(String.format("'%s' ", location));
		}
		if (origin != null) {
			action.append(String.format("at %s ", origin));
		}
		action.append("is correct");
		if (location != null && !location.isOptional()) {
			action.append(String.format(", or prefix it with '%s'", ConfigDataLocation.OPTIONAL_PREFIX));
		}
		return new FailureAnalysis(message, action.toString(), cause);
	}

	private ConfigDataLocation getLocation(ConfigDataNotFoundException cause) {
		if (cause instanceof ConfigDataLocationNotFoundException locationNotFoundException) {
			return locationNotFoundException.getLocation();
		}
		if (cause instanceof ConfigDataResourceNotFoundException resourceNotFoundException) {
			return resourceNotFoundException.getLocation();
		}
		return null;
	}

}
