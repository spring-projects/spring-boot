/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.autoconfigure.graphql;

import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;

/**
 * An implementation of {@link AbstractFailureAnalyzer} to analyze failures caused by
 * {@link InvalidSchemaLocationsException}.
 *
 * @author Brian Clozel
 */
class InvalidSchemaLocationsExceptionFailureAnalyzer extends AbstractFailureAnalyzer<InvalidSchemaLocationsException> {

	@Override
	protected FailureAnalysis analyze(Throwable rootFailure, InvalidSchemaLocationsException cause) {
		String message = "Could not find any GraphQL schema file under configured locations.";
		StringBuilder action = new StringBuilder(
				"Check that the following locations contain schema files: " + System.lineSeparator());
		for (InvalidSchemaLocationsException.SchemaLocation schemaLocation : cause.getSchemaLocations()) {
			action.append(String.format("- '%s' (%s)" + System.lineSeparator(), schemaLocation.getUri(),
					schemaLocation.getLocation()));
		}
		return new FailureAnalysis(message, action.toString(), cause);
	}

}
