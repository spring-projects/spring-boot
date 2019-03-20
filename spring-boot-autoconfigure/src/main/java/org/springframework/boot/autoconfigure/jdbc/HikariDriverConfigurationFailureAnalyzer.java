/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.autoconfigure.jdbc;

import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;

/**
 * An {@link AbstractFailureAnalyzer} that performs analysis of a Hikari configuration
 * failure caused by the use of the unsupported 'dataSourceClassName' property.
 *
 * @author Stephane Nicoll
 */
class HikariDriverConfigurationFailureAnalyzer
		extends AbstractFailureAnalyzer<IllegalStateException> {

	private static final String EXPECTED_MESSAGE = "both driverClassName and "
			+ "dataSourceClassName are specified, one or the other should be used";

	@Override
	protected FailureAnalysis analyze(Throwable rootFailure,
			IllegalStateException cause) {
		if (!EXPECTED_MESSAGE.equals(cause.getMessage())) {
			return null;
		}
		return new FailureAnalysis(
				"Configuration of the Hikari connection pool failed: "
						+ "'dataSourceClassName' is not supported.",
				"Spring Boot auto-configures only a driver and can't specify a custom "
						+ "DataSource. Consider configuring the Hikari DataSource in "
						+ "your own configuration.",
				cause);
	}

}
