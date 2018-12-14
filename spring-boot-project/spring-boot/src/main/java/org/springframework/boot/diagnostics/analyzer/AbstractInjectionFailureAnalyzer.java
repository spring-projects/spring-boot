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

import org.springframework.beans.BeanInstantiationException;
import org.springframework.beans.factory.InjectionPoint;
import org.springframework.beans.factory.UnsatisfiedDependencyException;
import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.boot.diagnostics.FailureAnalyzer;
import org.springframework.util.ClassUtils;

/**
 * Abstract base class for a {@link FailureAnalyzer} that handles some kind of injection
 * failure.
 *
 * @param <T> the type of exception to analyze
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @since 1.4.1
 */
public abstract class AbstractInjectionFailureAnalyzer<T extends Throwable>
		extends AbstractFailureAnalyzer<T> {

	@Override
	protected final FailureAnalysis analyze(Throwable rootFailure, T cause) {
		return analyze(rootFailure, cause, getDescription(rootFailure));
	}

	private String getDescription(Throwable rootFailure) {
		UnsatisfiedDependencyException unsatisfiedDependency = findMostNestedCause(
				rootFailure, UnsatisfiedDependencyException.class);
		if (unsatisfiedDependency != null) {
			return getDescription(unsatisfiedDependency);
		}
		BeanInstantiationException beanInstantiationException = findMostNestedCause(
				rootFailure, BeanInstantiationException.class);
		if (beanInstantiationException != null) {
			return getDescription(beanInstantiationException);
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	private <C extends Exception> C findMostNestedCause(Throwable root, Class<C> type) {
		Throwable candidate = root;
		C result = null;
		while (candidate != null) {
			if (type.isAssignableFrom(candidate.getClass())) {
				result = (C) candidate;
			}
			candidate = candidate.getCause();
		}
		return result;
	}

	private String getDescription(UnsatisfiedDependencyException ex) {
		InjectionPoint injectionPoint = ex.getInjectionPoint();
		if (injectionPoint != null) {
			if (injectionPoint.getField() != null) {
				return String.format("Field %s in %s",
						injectionPoint.getField().getName(),
						injectionPoint.getField().getDeclaringClass().getName());
			}
			if (injectionPoint.getMethodParameter() != null) {
				if (injectionPoint.getMethodParameter().getConstructor() != null) {
					return String.format("Parameter %d of constructor in %s",
							injectionPoint.getMethodParameter().getParameterIndex(),
							injectionPoint.getMethodParameter().getDeclaringClass()
									.getName());
				}
				return String.format("Parameter %d of method %s in %s",
						injectionPoint.getMethodParameter().getParameterIndex(),
						injectionPoint.getMethodParameter().getMethod().getName(),
						injectionPoint.getMethodParameter().getDeclaringClass()
								.getName());
			}
		}
		return ex.getResourceDescription();
	}

	private String getDescription(BeanInstantiationException ex) {
		if (ex.getConstructingMethod() != null) {
			return String.format("Method %s in %s", ex.getConstructingMethod().getName(),
					ex.getConstructingMethod().getDeclaringClass().getName());
		}
		if (ex.getConstructor() != null) {
			return String.format("Constructor in %s", ClassUtils
					.getUserClass(ex.getConstructor().getDeclaringClass()).getName());
		}
		return ex.getBeanClass().getName();
	}

	/**
	 * Returns an analysis of the given {@code rootFailure}, or {@code null} if no
	 * analysis was possible.
	 * @param rootFailure the root failure passed to the analyzer
	 * @param cause the actual found cause
	 * @param description the description of the injection point or {@code null}
	 * @return the analysis or {@code null}
	 */
	protected abstract FailureAnalysis analyze(Throwable rootFailure, T cause,
			String description);

}
