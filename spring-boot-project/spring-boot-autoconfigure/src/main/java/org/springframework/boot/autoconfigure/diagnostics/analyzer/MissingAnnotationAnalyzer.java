/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.autoconfigure.diagnostics.analyzer;

import java.util.Arrays;
import java.util.Set;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.context.event.FailedStartupInfoHolder;
import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.context.ApplicationContextException;

/**
 * Provides some diagnostic abilities to try and determine if the developer missed adding SpringBootApplication to the relevant
 * primary source class used during boot.
 *
 * @author Dan King
 */
public class MissingAnnotationAnalyzer extends AbstractFailureAnalyzer<ApplicationContextException> {

	private static final String ISSUE_DESCRIPTION = "Missing %s annotation on main class.";

	private static final String ACTION_DESCRIPTION = "Add %s to %s or the main primary source class.";

	@Override
	protected FailureAnalysis analyze(Throwable rootFailure, ApplicationContextException cause) {
		Set<Object> sources = FailedStartupInfoHolder.getAllSources();

		for (Object source : sources) {
			if (source instanceof Class) {
				Class type = (Class) source;

				if (Arrays.stream(type.getAnnotations())
						.anyMatch(annotation -> annotation.annotationType().equals(SpringBootApplication.class))) {
					return null;
				}
			}
		}

		final String mainApplicationName = FailedStartupInfoHolder.getMainApplication().getName();

		return new FailureAnalysis(String.format(ISSUE_DESCRIPTION, SpringBootApplication.class.getName()),
				String.format(ACTION_DESCRIPTION, SpringBootApplication.class.getName(), mainApplicationName), cause);
	}

}
