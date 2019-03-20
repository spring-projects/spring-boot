/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.diagnostics;

import org.springframework.core.ResolvableType;

/**
 * Abstract base class for most {@code FailureAnalyzer} implementations.
 *
 * @param <T> the type of exception to analyze
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @since 1.4.0
 */
public abstract class AbstractFailureAnalyzer<T extends Throwable>
		implements FailureAnalyzer {

	@Override
	public FailureAnalysis analyze(Throwable failure) {
		T cause = findCause(failure, getCauseType());
		if (cause != null) {
			return analyze(failure, cause);
		}
		return null;
	}

	/**
	 * Returns an analysis of the given {@code failure}, or {@code null} if no analysis
	 * was possible.
	 * @param rootFailure the root failure passed to the analyzer
	 * @param cause the actual found cause
	 * @return the analysis or {@code null}
	 */
	protected abstract FailureAnalysis analyze(Throwable rootFailure, T cause);

	/**
	 * Return the cause type being handled by the analyzer. By default the class generic
	 * is used.
	 * @return the cause type
	 */
	@SuppressWarnings("unchecked")
	protected Class<? extends T> getCauseType() {
		return (Class<? extends T>) ResolvableType
				.forClass(AbstractFailureAnalyzer.class, getClass()).resolveGeneric();
	}

	@SuppressWarnings("unchecked")
	protected final <E extends Throwable> T findCause(Throwable failure, Class<E> type) {
		while (failure != null) {
			if (type.isInstance(failure)) {
				return (T) failure;
			}
			failure = failure.getCause();
		}
		return null;
	}

}
