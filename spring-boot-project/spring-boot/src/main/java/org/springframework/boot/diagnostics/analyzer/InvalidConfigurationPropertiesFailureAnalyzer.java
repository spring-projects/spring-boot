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

import java.lang.reflect.Constructor;

import org.springframework.boot.context.properties.InvalidConfigurationPropertiesException;
import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;

/**
 * An {@link AbstractFailureAnalyzer} that performs analysis of failures caused by
 * {@link InvalidConfigurationPropertiesException}.
 *
 * @author Madhura Bhave
 * @author Stephane Nicoll
 * @since 2.2.0
 */
public class InvalidConfigurationPropertiesFailureAnalyzer
		extends AbstractFailureAnalyzer<InvalidConfigurationPropertiesException> {

	@Override
	protected FailureAnalysis analyze(Throwable rootFailure, InvalidConfigurationPropertiesException cause) {
		Class<?> target = cause.getConfigurationProperties();
		Constructor<?> autowiringConstructor = getAutowiringConstructor(target);
		String componentName = cause.getComponent().getSimpleName();
		return new FailureAnalysis(getDescription(target, autowiringConstructor, componentName),
				getAction(target, autowiringConstructor, componentName), cause);
	}

	private String getDescription(Class<?> target, Constructor<?> autowiringConstructor, String componentName) {
		String targetName = target.getSimpleName();
		StringBuilder sb = new StringBuilder(targetName);
		sb.append(" is annotated with @ConfigurationProperties and @").append(componentName)
				.append(". This may cause the @ConfigurationProperties bean to be registered twice.");
		if (autowiringConstructor != null) {
			sb.append(" Also, autowiring by constructor is enabled for ").append(targetName)
					.append(" which conflicts with properties constructor binding.");
		}
		return sb.toString();
	}

	private String getAction(Class<?> target, Constructor<?> autowiringConstructor, String componentName) {
		StringBuilder sb = new StringBuilder();
		if (autowiringConstructor != null) {
			sb.append("Consider refactoring ").append(target.getSimpleName()).append(
					" so that it does not rely on other beans. Alternatively, a default constructor should be added and @Autowired should be defined on ")
					.append(autowiringConstructor.toGenericString()).append(String.format(".%n%n"));
		}
		sb.append("Remove @").append(componentName).append(" from ").append(target.getName())
				.append(" or consider disabling automatic @ConfigurationProperties scanning.");
		return sb.toString();
	}

	private Constructor<?> getAutowiringConstructor(Class<?> target) {
		Constructor<?>[] candidates = target.getDeclaredConstructors();
		if (candidates.length == 1) {
			Constructor<?> candidate = candidates[0];
			if (candidate.getParameterCount() > 0) {
				return candidate;
			}
		}
		return null;
	}

}
