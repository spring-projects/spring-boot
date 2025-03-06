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

package org.springframework.boot.autoconfigure.r2dbc;

import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.boot.diagnostics.FailureAnalyzer;

/**
 * {@link FailureAnalyzer} for {@link MissingR2dbcPoolDependencyException}.
 *
 * @author Andy Wilkinson
 */
class MissingR2dbcPoolDependencyFailureAnalyzer extends AbstractFailureAnalyzer<MissingR2dbcPoolDependencyException> {

	@Override
	protected FailureAnalysis analyze(Throwable rootFailure, MissingR2dbcPoolDependencyException cause) {
		return new FailureAnalysis(cause.getMessage(),
				"Update your application's build to depend on io.r2dbc:r2dbc-pool or your application's configuration "
						+ "to disable R2DBC connection pooling.",
				cause);
	}

}
