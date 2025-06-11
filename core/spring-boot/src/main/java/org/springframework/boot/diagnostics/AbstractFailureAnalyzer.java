/*
 * Copyright 2012-present the original author or authors.
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

import org.jspecify.annotations.Nullable;

import org.springframework.core.ResolvableType;
import org.springframework.util.Assert;

/**
 * Abstract base class for most {@code FailureAnalyzer} implementations.
 *
 * @param <T> the type of exception to analyze
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @since 1.4.0
 */
public abstract class AbstractFailureAnalyzer<T extends Throwable> implements FailureAnalyzer {

	@Override
	public @Nullable FailureAnalysis analyze(Throwable failure) {
		T cause = findCause(failure, getCauseType());
		return (cause != null) ? analyze(failure, cause) : null;
	}

	/**
	 * Returns an analysis of the given {@code rootFailure}, or {@code null} if no
	 * analysis was possible.
	 * @param rootFailure the root failure passed to the analyzer
	 * @param cause the actual found cause
	 * @return the analysis or {@code null}
	 */
	protected abstract @Nullable FailureAnalysis analyze(Throwable rootFailure, T cause);

	/**
	 * Return the cause type being handled by the analyzer. By default the class generic
	 * is used.
	 * @return the cause type
	 */
	@SuppressWarnings("unchecked")
	protected Class<? extends T> getCauseType() {
		Class<? extends T> type = (Class<? extends T>) ResolvableType
			.forClass(AbstractFailureAnalyzer.class, getClass())
			.resolveGeneric();
		Assert.state(type != null, "Unable to resolve generic");
		return type;
	}

	@SuppressWarnings("unchecked")
	protected final <E extends Throwable> @Nullable E findCause(Throwable failure, Class<E> type) {
		while (failure != null) {
			if (type.isInstance(failure)) {
				return (E) failure;
			}
			failure = failure.getCause();
		}
		return null;
	}

}
