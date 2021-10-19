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
 * {@link FailureAnalyzer} for {@link MultipleConnectionPoolConfigurationsException}.
 *
 * @author Andy Wilkinson
 */
class MultipleConnectionPoolConfigurationsFailureAnalzyer
		extends AbstractFailureAnalyzer<MultipleConnectionPoolConfigurationsException> {

	@Override
	protected FailureAnalysis analyze(Throwable rootFailure, MultipleConnectionPoolConfigurationsException cause) {
		return new FailureAnalysis(cause.getMessage(),
				"Update your configuration so that R2DBC connection pooling is configured using either the "
						+ "spring.r2dbc.url property or the spring.r2dbc.pool.* properties",
				cause);
	}

}
