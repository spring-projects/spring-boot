/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jooq.autoconfigure;

import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.boot.diagnostics.FailureAnalyzer;

/**
 * {@link FailureAnalyzer} for {@link JaxbNotAvailableException}.
 *
 * @author Moritz Halbritter
 */
class JaxbNotAvailableExceptionFailureAnalyzer extends AbstractFailureAnalyzer<JaxbNotAvailableException> {

	@Override
	protected FailureAnalysis analyze(Throwable rootFailure, JaxbNotAvailableException cause) {
		return new FailureAnalysis("Unable to unmarshal jOOQ settings because JAXB is not available.",
				"Add JAXB to the classpath or remove the spring.jooq.config property.", cause);
	}

}
