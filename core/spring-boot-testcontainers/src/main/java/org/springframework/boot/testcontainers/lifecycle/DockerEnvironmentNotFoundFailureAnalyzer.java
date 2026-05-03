/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.testcontainers.lifecycle;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.util.StringUtils;

/**
 * A {@code FailureAnalyzer} that performs analysis of failures caused by Testcontainers
 * not finding a valid Docker environment.
 *
 * @author Dmytro Nosan
 * @author Stephane Nicoll
 */
class DockerEnvironmentNotFoundFailureAnalyzer extends AbstractFailureAnalyzer<IllegalStateException> {

	private static final String EXPECTED_MESSAGE = "Could not find a valid Docker environment";

	private static final String DESCRIPTION = "Could not find a valid Docker environment for Testcontainers.";

	private static final String ACTION = """
			    - Ensure a Docker-compatible container engine is installed and running.
			    - If running Testcontainers in CI, ensure the runner has access to the daemon, typically by using a mounted socket or a Docker-in-Docker setup.
			    - Review the Testcontainers documentation for troubleshooting and advanced configuration options.
			""";

	@Override
	protected @Nullable FailureAnalysis analyze(Throwable rootFailure, IllegalStateException cause) {
		if (StringUtils.hasText(cause.getMessage()) && cause.getMessage().contains(EXPECTED_MESSAGE)) {
			return new FailureAnalysis(DESCRIPTION, ACTION, cause);
		}
		return null;
	}

}
