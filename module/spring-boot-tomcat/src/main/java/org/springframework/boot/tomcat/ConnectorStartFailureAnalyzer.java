/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.tomcat;

import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;

/**
 * An {@link AbstractFailureAnalyzer} for {@link ConnectorStartFailedException}.
 *
 * @author Andy Wilkinson
 */
class ConnectorStartFailureAnalyzer extends AbstractFailureAnalyzer<ConnectorStartFailedException> {

	@Override
	protected FailureAnalysis analyze(Throwable rootFailure, ConnectorStartFailedException cause) {
		return new FailureAnalysis(
				"The Tomcat connector configured to listen on port " + cause.getPort()
						+ " failed to start. The port may already be in use or the connector may be misconfigured.",
				"Verify the connector's configuration, identify and stop any process that's listening on port "
						+ cause.getPort() + ", or configure this application to listen on another port.",
				cause);
	}

}
