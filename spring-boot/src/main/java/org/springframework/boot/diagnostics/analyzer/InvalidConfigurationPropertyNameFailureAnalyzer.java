/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.diagnostics.analyzer;

import java.util.stream.Collectors;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.context.properties.source.InvalidConfigurationPropertyNameException;
import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;

/**
 * An {@link AbstractFailureAnalyzer} that performs analysis of failures caused by
 * {@link InvalidConfigurationPropertyNameException}.
 *
 * @author Madhura Bhave
 * @since 2.0.0
 */
public class InvalidConfigurationPropertyNameFailureAnalyzer
		extends AbstractFailureAnalyzer<InvalidConfigurationPropertyNameException> {

	@Override
	protected FailureAnalysis analyze(Throwable rootFailure,
			InvalidConfigurationPropertyNameException cause) {
		BeanCreationException exception = findCause(rootFailure,
				BeanCreationException.class);
		String action = String.format(
				"Modify '%s' so that it conforms to the canonical names requirements.",
				cause.getName());
		return new FailureAnalysis(buildDescription(cause, exception), action, cause);
	}

	private String buildDescription(InvalidConfigurationPropertyNameException cause,
			BeanCreationException exception) {
		StringBuilder description = new StringBuilder(String.format(
				"Configuration property name '%s' is not valid:%n", cause.getName()));
		String invalid = cause.getInvalidCharacters().stream().map(this::quote)
				.collect(Collectors.joining(", "));
		description.append(String.format("%n    Invalid characters: %s", invalid));
		if (exception != null) {
			description.append(String.format("%n    Bean: %s", exception.getBeanName()));
		}
		description.append(String.format("%n    Reason: Canonical names should be "
				+ "kebab-case ('-' separated), lowercase alpha-numeric characters"
				+ " and must start with a letter"));
		return description.toString();
	}

	private String quote(Character c) {
		return "'" + c + "'";
	}

}
