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

package org.springframework.boot.diagnostics.analyzer;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanCurrentlyInCreationException;
import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.util.StringUtils;

/**
 * A {@link AbstractFailureAnalyzer} the performs analysis of failures caused by a
 * {@link BeanCurrentlyInCreationException}.
 *
 * @author Andy Wilkinson
 */
class BeanCurrentlyInCreationFailureAnalyzer
		extends AbstractFailureAnalyzer<BeanCurrentlyInCreationException> {

	private static final String FIELD_AUTOWIRING_FAILURE_MESSAGE_PREFIX = "Could not autowire field: ";

	@Override
	protected FailureAnalysis analyze(Throwable rootFailure,
			BeanCurrentlyInCreationException cause) {
		List<String> beansInCycle = new ArrayList<String>();
		Throwable candidate = rootFailure;
		while (candidate != null) {
			if (candidate instanceof BeanCreationException) {
				BeanCreationException creationEx = (BeanCreationException) candidate;
				if (StringUtils.hasText(creationEx.getBeanName())) {
					beansInCycle
							.add(creationEx.getBeanName() + getDescription(creationEx));
				}
			}
			candidate = candidate.getCause();
		}
		StringBuilder message = new StringBuilder();
		int uniqueBeans = beansInCycle.size() - 1;
		message.append(
				String.format("There is a circular dependency between %s beans in the "
						+ "application context:%n", uniqueBeans));
		for (String bean : beansInCycle) {
			message.append(String.format("\t- %s%n", bean));
		}
		return new FailureAnalysis(message.toString(), null, cause);
	}

	private String getDescription(BeanCreationException ex) {
		if (StringUtils.hasText(ex.getResourceDescription())) {
			return String.format(" defined in %s", ex.getResourceDescription());
		}
		if (causedByFieldAutowiringFailure(ex)) {
			return String.format(" (field %s)",
					ex.getCause().getMessage().substring(
							FIELD_AUTOWIRING_FAILURE_MESSAGE_PREFIX.length(),
							ex.getCause().getMessage().indexOf(";")));

		}
		return "";
	}

	private boolean causedByFieldAutowiringFailure(BeanCreationException ex) {
		return ex.getCause() instanceof BeanCreationException && ex.getCause()
				.getMessage().startsWith(FIELD_AUTOWIRING_FAILURE_MESSAGE_PREFIX);
	}

}
