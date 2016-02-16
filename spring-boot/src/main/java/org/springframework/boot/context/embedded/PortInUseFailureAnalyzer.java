/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.context.embedded;

import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;

/**
 * A {@code FailureAnalyzer} that performs analysis of failures caused by a
 * {@code PortInUseException}.
 *
 * @author Andy Wilkinson
 * @since 1.4.0
 */
public class PortInUseFailureAnalyzer extends AbstractFailureAnalyzer {

	@Override
	public FailureAnalysis analyze(Throwable failure) {
		PortInUseException portInUseException = findFailure(failure,
				PortInUseException.class);
		if (portInUseException != null) {
			return new FailureAnalysis(
					"Embedded servlet container failed to start. Port "
							+ portInUseException.getPort() + " was already in use.",
					"Identify and stop the process that's listening on port "
							+ portInUseException.getPort() + " or configure this "
							+ "application to listen on another port.",
					portInUseException);
		}
		return null;
	}

}
