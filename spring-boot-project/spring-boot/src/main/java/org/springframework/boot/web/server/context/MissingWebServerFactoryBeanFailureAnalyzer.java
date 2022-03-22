/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.web.server.context;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.boot.diagnostics.FailureAnalyzer;
import org.springframework.context.ApplicationContextException;

/**
 * A {@link FailureAnalyzer} that performs analysis of failures caused by an
 * {@link MissingWebServerFactoryBeanException}.
 *
 * @author Guirong Hu
 */
class MissingWebServerFactoryBeanFailureAnalyzer extends AbstractFailureAnalyzer<ApplicationContextException> {

	private static final String ACTION = "Check your application's dependencies on supported web servers "
			+ "or configuration of web application type.";

	@Override
	protected FailureAnalysis analyze(Throwable rootFailure, ApplicationContextException cause) {
		Throwable rootCause = cause.getCause();
		if (rootCause instanceof MissingWebServerFactoryBeanException) {
			WebApplicationType webApplicationType = ((MissingWebServerFactoryBeanException) rootCause)
					.getWebApplicationType();
			return new FailureAnalysis(String.format(
					"Reason: The running web application is of type %s, but the dependent class is missing.",
					webApplicationType.name().toLowerCase()), ACTION, cause);
		}
		return null;
	}

}
