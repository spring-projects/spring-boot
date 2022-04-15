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

package org.springframework.boot.web.context;

import java.util.Locale;

import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.boot.diagnostics.FailureAnalyzer;
import org.springframework.core.annotation.Order;

/**
 * A {@link FailureAnalyzer} that performs analysis of failures caused by a
 * {@link MissingWebServerFactoryBeanException}.
 *
 * @author Guirong Hu
 * @author Andy Wilkinson
 */
@Order(0)
class MissingWebServerFactoryBeanFailureAnalyzer extends AbstractFailureAnalyzer<MissingWebServerFactoryBeanException> {

	@Override
	protected FailureAnalysis analyze(Throwable rootFailure, MissingWebServerFactoryBeanException cause) {
		return new FailureAnalysis(
				"Web application could not be started as there was no " + cause.getBeanType().getName()
						+ " bean defined in the context.",
				"Check your application's dependencies for a supported "
						+ cause.getWebApplicationType().name().toLowerCase(Locale.ENGLISH) + " web server.\n"
						+ "Check the configured web application type.",
				cause);
	}

}
