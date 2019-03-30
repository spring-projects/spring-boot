/*
 * Copyright 2012-2018 the original author or authors.
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

import java.io.PrintWriter;
import java.io.StringWriter;

import org.springframework.beans.factory.support.BeanDefinitionOverrideException;
import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;

/**
 * An {@link AbstractFailureAnalyzer} that performs analysis of failures caused by a
 * {@link BeanDefinitionOverrideException}.
 *
 * @author Andy Wilkinson
 */
class BeanDefinitionOverrideFailureAnalyzer
		extends AbstractFailureAnalyzer<BeanDefinitionOverrideException> {

	private static final String ACTION = "Consider renaming one of the beans or enabling "
			+ "overriding by setting spring.main.allow-bean-definition-overriding=true";

	@Override
	protected FailureAnalysis analyze(Throwable rootFailure,
			BeanDefinitionOverrideException cause) {
		return new FailureAnalysis(getDescription(cause), ACTION, cause);
	}

	private String getDescription(BeanDefinitionOverrideException ex) {
		StringWriter description = new StringWriter();
		PrintWriter printer = new PrintWriter(description);
		printer.printf(
				"The bean '%s', defined in %s, could not be registered. A bean with that "
						+ "name has already been defined in %s and overriding is disabled.",
				ex.getBeanName(), ex.getBeanDefinition().getResourceDescription(),
				ex.getExistingDefinition().getResourceDescription());
		return description.toString();
	}

}
