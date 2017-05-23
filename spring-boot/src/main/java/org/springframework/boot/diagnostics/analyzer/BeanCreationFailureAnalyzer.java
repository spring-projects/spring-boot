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

package org.springframework.boot.diagnostics.analyzer;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.boot.diagnostics.FailureAnalyzer;
import org.springframework.core.annotation.Order;

/**
 * A {@link FailureAnalyzer} that performs analysis of failures caused by a
 * {@link BeanCreationException}.
 *
 * @author Stephane Nicoll
 * @see BeanCurrentlyInCreationFailureAnalyzer
 */
@Order(100)
public class BeanCreationFailureAnalyzer
		extends AbstractFailureAnalyzer<BeanCreationException> {

	@Override
	protected FailureAnalysis analyze(Throwable rootFailure,
			BeanCreationException cause) {
		StringBuilder sb = new StringBuilder();
		sb.append("A bean named '").append(cause.getBeanName()).append("'");
		if (cause.getResourceDescription() != null) {
			sb.append(" defined in ").append(cause.getResourceDescription());
		}
		sb.append(" failed to be created:");
		sb.append(String.format("%n%n"));
		Throwable nested = findMostNestedCause(cause);
		sb.append(String.format("\t%s", nested.getMessage()));
		return new FailureAnalysis(sb.toString(), null, cause);
	}

	private Throwable findMostNestedCause(Throwable exception) {
		if (exception.getCause() == null) {
			return exception;
		}
		return findMostNestedCause(exception.getCause());
	}

}
