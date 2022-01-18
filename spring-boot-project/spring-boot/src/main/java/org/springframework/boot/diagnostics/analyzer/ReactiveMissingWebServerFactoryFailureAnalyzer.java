/*
 * Copyright 2012-2019 the original author or authors.
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
import org.springframework.context.ApplicationContextException;
import org.springframework.util.ClassUtils;

/**
 * A {@code FailureAnalyzer} that performs analysis of failures caused by a
 * {@code ApplicationContextException}.
 *
 * @author Zhao Xianming
 */
class ReactiveMissingWebServerFactoryFailureAnalyzer extends AbstractFailureAnalyzer<ApplicationContextException> {

	private static final String MISSING_WEBSERVER_FACTORY_MESSAGE = "due to missing ReactiveWebServerFactory bean.";

	private static final String REACTIVE_WEB_APPLICATION_CLASS = "org.springframework.web.reactive.HandlerResult";

	@Override
	protected FailureAnalysis analyze(Throwable rootFailure, ApplicationContextException cause) {
		if (cause.getMessage().contains(MISSING_WEBSERVER_FACTORY_MESSAGE)
				&& !ClassUtils.isPresent(REACTIVE_WEB_APPLICATION_CLASS, null)) {
			return new FailureAnalysis(
					"The web-application_type is set to reactive but webflux related classes are missing.",
					"Add spring-boot-starter-webflux dependency.", cause);
		}
		return null;
	}

}
