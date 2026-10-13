/*
 * Copyright 2012-present the original author or authors.
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
