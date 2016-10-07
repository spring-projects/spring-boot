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
import org.springframework.beans.factory.InjectionPoint;
import org.springframework.beans.factory.UnsatisfiedDependencyException;
import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.util.StringUtils;

/**
 * An {@link AbstractFailureAnalyzer} that performs analysis of failures caused by a
 * {@link BeanCurrentlyInCreationException}.
 *
 * @author Andy Wilkinson
 */
class BeanCurrentlyInCreationFailureAnalyzer
		extends AbstractFailureAnalyzer<BeanCurrentlyInCreationException> {

	@Override
	protected FailureAnalysis analyze(Throwable rootFailure,
			BeanCurrentlyInCreationException cause) {
		List<BeanInCycle> beansInCycle = new ArrayList<BeanInCycle>();
		Throwable candidate = rootFailure;
		int cycleStart = -1;
		while (candidate != null) {
			if (candidate instanceof BeanCreationException) {
				BeanCreationException creationEx = (BeanCreationException) candidate;
				if (StringUtils.hasText(creationEx.getBeanName())) {
					BeanInCycle beanInCycle = new BeanInCycle(creationEx);
					int index = beansInCycle.indexOf(beanInCycle);
					if (index == -1) {
						beansInCycle.add(beanInCycle);
					}
					else {
						cycleStart = index;
					}
				}
			}
			candidate = candidate.getCause();
		}
		StringBuilder message = new StringBuilder();
		message.append(String.format("The dependencies of some of the beans in the "
				+ "application context form a cycle:%n%n"));
		for (int i = 0; i < beansInCycle.size(); i++) {
			if (i == cycleStart) {
				message.append(String.format("┌─────┐%n"));
			}
			else if (i > 0) {
				message.append(String.format("%s     ↓%n", i < cycleStart ? " " : "↑"));
			}
			message.append(String.format("%s  %s%n", i < cycleStart ? " " : "|",
					beansInCycle.get(i)));
		}
		message.append(String.format("└─────┘%n"));
		return new FailureAnalysis(message.toString(), null, cause);
	}

	private static final class BeanInCycle {

		private final String name;

		private final String description;

		private BeanInCycle(BeanCreationException ex) {
			this.name = ex.getBeanName();
			this.description = determineDescription(ex);
		}

		private String determineDescription(BeanCreationException ex) {
			if (StringUtils.hasText(ex.getResourceDescription())) {
				return String.format(" defined in %s", ex.getResourceDescription());
			}
			InjectionPoint failedInjectionPoint = findFailedInjectionPoint(ex);
			if (failedInjectionPoint != null && failedInjectionPoint.getField() != null) {
				return String.format(" (field %s)", failedInjectionPoint.getField());
			}
			return "";
		}

		private InjectionPoint findFailedInjectionPoint(BeanCreationException ex) {
			if (!(ex instanceof UnsatisfiedDependencyException)) {
				return null;
			}
			return ((UnsatisfiedDependencyException) ex).getInjectionPoint();
		}

		@Override
		public int hashCode() {
			return this.name.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			BeanInCycle other = (BeanInCycle) obj;
			return this.name.equals(other.name);
		}

		@Override
		public String toString() {
			return this.name + this.description;
		}

	}

}
