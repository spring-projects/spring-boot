/*
 * Copyright 2012-2017 the original author or authors.
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

import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.boot.web.embedded.tomcat.ConnectorStartFailedException;

/**
 * An {@link AbstractFailureAnalyzer} for {@link ConnectorStartFailedException}.
 *
 * @author Andy Wilkinson
 */
class ConnectorStartFailureAnalyzer
		extends AbstractFailureAnalyzer<ConnectorStartFailedException> {

	@Override
	protected FailureAnalysis analyze(Throwable rootFailure,
			ConnectorStartFailedException cause) {
		return new FailureAnalysis(
				"The Tomcat connector configured to listen on port " + cause.getPort()
						+ " failed to start. The port may already be in use or the"
						+ " connector may be misconfigured.",
				"Verify the connector's configuration, identify and stop any process "
						+ "that's listening on port " + cause.getPort()
						+ ", or configure this application to listen on another port.",
				cause);
	}

}
