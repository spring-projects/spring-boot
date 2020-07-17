/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.liquibase;

import java.io.FileNotFoundException;

import liquibase.exception.ChangeLogParseException;

import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.util.StringUtils;

/**
 * An {@link AbstractFailureAnalyzer} that analyzes exceptions of type
 * {@link ChangeLogParseException} caused by a Liquibase changelog not being present.
 *
 * @author Sebastiaan Fernandez
 */
class LiquibaseChangelogMissingFailureAnalyzer extends AbstractFailureAnalyzer<ChangeLogParseException> {

	@Override
	protected FailureAnalysis analyze(Throwable rootFailure, ChangeLogParseException cause) {
		FileNotFoundException fileNotFound = findCause(cause, FileNotFoundException.class);
		if (fileNotFound != null) {
			String changelogPath = extractChangelogPath(cause);
			if (StringUtils.hasText(changelogPath)) {
				return new FailureAnalysis(getDescription(changelogPath),
						"Make sure a Liquibase changelog is present at the configured path.", cause);
			}
		}
		return null;
	}

	private String extractChangelogPath(ChangeLogParseException cause) {
		return cause.getMessage().substring("Error parsing ".length());
	}

	private String getDescription(String changelogPath) {
		return "Liquibase failed to start because no changelog could be found at '" + changelogPath + "'.";
	}

}
