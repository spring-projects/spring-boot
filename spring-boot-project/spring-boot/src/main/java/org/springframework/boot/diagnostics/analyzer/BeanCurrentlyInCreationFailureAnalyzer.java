/*
 * Copyright 2012-2022 the original author or authors.
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

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanCurrentlyInCreationException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.InjectionPoint;
import org.springframework.beans.factory.UnsatisfiedDependencyException;
import org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory;
import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.util.StringUtils;

/**
 * An {@link AbstractFailureAnalyzer} that performs analysis of failures caused by a
 * {@link BeanCurrentlyInCreationException}.
 *
 * @author Andy Wilkinson
 * @author Scott Frederick
 */
class BeanCurrentlyInCreationFailureAnalyzer extends AbstractFailureAnalyzer<BeanCurrentlyInCreationException> {

	private final AbstractAutowireCapableBeanFactory beanFactory;

	/**
     * Constructs a new BeanCurrentlyInCreationFailureAnalyzer with the specified beanFactory.
     * 
     * @param beanFactory the bean factory to be used by the analyzer
     */
    BeanCurrentlyInCreationFailureAnalyzer(BeanFactory beanFactory) {
		if (beanFactory instanceof AbstractAutowireCapableBeanFactory autowireCapableBeanFactory) {
			this.beanFactory = autowireCapableBeanFactory;
		}
		else {
			this.beanFactory = null;
		}
	}

	/**
     * Analyzes the failure caused by a bean currently in creation exception.
     * 
     * @param rootFailure the root cause of the failure
     * @param cause the bean currently in creation exception
     * @return a FailureAnalysis object containing information about the failure, or null if no cycle is found
     */
    @Override
	protected FailureAnalysis analyze(Throwable rootFailure, BeanCurrentlyInCreationException cause) {
		DependencyCycle dependencyCycle = findCycle(rootFailure);
		if (dependencyCycle == null) {
			return null;
		}
		return new FailureAnalysis(buildMessage(dependencyCycle), action(), cause);
	}

	/**
     * Returns a message indicating the action to be taken when a dependency cycle between beans is detected.
     * 
     * @return A message indicating the action to be taken
     */
    private String action() {
		if (this.beanFactory != null && this.beanFactory.isAllowCircularReferences()) {
			return "Despite circular references being allowed, the dependency cycle between beans could not be "
					+ "broken. Update your application to remove the dependency cycle.";
		}
		return "Relying upon circular references is discouraged and they are prohibited by default. "
				+ "Update your application to remove the dependency cycle between beans. "
				+ "As a last resort, it may be possible to break the cycle automatically by setting "
				+ "spring.main.allow-circular-references to true.";
	}

	/**
     * Finds the dependency cycle in the given root failure.
     * 
     * @param rootFailure the root failure to analyze
     * @return the DependencyCycle object representing the cycle, or null if no cycle is found
     */
    private DependencyCycle findCycle(Throwable rootFailure) {
		List<BeanInCycle> beansInCycle = new ArrayList<>();
		Throwable candidate = rootFailure;
		int cycleStart = -1;
		while (candidate != null) {
			BeanInCycle beanInCycle = BeanInCycle.get(candidate);
			if (beanInCycle != null) {
				int index = beansInCycle.indexOf(beanInCycle);
				if (index == -1) {
					beansInCycle.add(beanInCycle);
				}
				cycleStart = (cycleStart != -1) ? cycleStart : index;
			}
			candidate = candidate.getCause();
		}
		if (cycleStart == -1) {
			return null;
		}
		return new DependencyCycle(beansInCycle, cycleStart);
	}

	/**
     * Builds a message describing the dependency cycle in the application context.
     * 
     * @param dependencyCycle the DependencyCycle object representing the cycle
     * @return the message describing the dependency cycle
     */
    private String buildMessage(DependencyCycle dependencyCycle) {
		StringBuilder message = new StringBuilder();
		message.append(
				String.format("The dependencies of some of the beans in the application context form a cycle:%n%n"));
		List<BeanInCycle> beansInCycle = dependencyCycle.getBeansInCycle();
		boolean singleBean = beansInCycle.size() == 1;
		int cycleStart = dependencyCycle.getCycleStart();
		for (int i = 0; i < beansInCycle.size(); i++) {
			BeanInCycle beanInCycle = beansInCycle.get(i);
			if (i == cycleStart) {
				message.append(String.format(singleBean ? "┌──->──┐%n" : "┌─────┐%n"));
			}
			else if (i > 0) {
				String leftSide = (i < cycleStart) ? " " : "↑";
				message.append(String.format("%s     ↓%n", leftSide));
			}
			String leftSide = (i < cycleStart) ? " " : "|";
			message.append(String.format("%s  %s%n", leftSide, beanInCycle));
		}
		message.append(String.format(singleBean ? "└──<-──┘%n" : "└─────┘%n"));
		return message.toString();
	}

	/**
     * DependencyCycle class.
     */
    private static final class DependencyCycle {

		private final List<BeanInCycle> beansInCycle;

		private final int cycleStart;

		/**
         * Constructs a new DependencyCycle object with the specified list of beans in cycle and cycle start index.
         * 
         * @param beansInCycle the list of beans in the cycle
         * @param cycleStart the index of the cycle start
         */
        private DependencyCycle(List<BeanInCycle> beansInCycle, int cycleStart) {
			this.beansInCycle = beansInCycle;
			this.cycleStart = cycleStart;
		}

		/**
         * Returns the list of beans in the cycle.
         *
         * @return the list of beans in the cycle
         */
        List<BeanInCycle> getBeansInCycle() {
			return this.beansInCycle;
		}

		/**
         * Returns the start of the cycle.
         *
         * @return the start of the cycle
         */
        int getCycleStart() {
			return this.cycleStart;
		}

	}

	/**
     * BeanInCycle class.
     */
    private static final class BeanInCycle {

		private final String name;

		private final String description;

		/**
         * Creates a new BeanInCycle object with the given BeanCreationException.
         * 
         * @param ex the BeanCreationException that occurred
         * @return a new BeanInCycle object
         */
        private BeanInCycle(BeanCreationException ex) {
			this.name = ex.getBeanName();
			this.description = determineDescription(ex);
		}

		/**
         * Determines the description of the given BeanCreationException.
         * 
         * @param ex the BeanCreationException to determine the description for
         * @return the description of the BeanCreationException
         */
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

		/**
         * Finds the failed injection point from the given BeanCreationException.
         * 
         * @param ex the BeanCreationException that occurred
         * @return the InjectionPoint object representing the failed injection point, or null if not found
         */
        private InjectionPoint findFailedInjectionPoint(BeanCreationException ex) {
			if (ex instanceof UnsatisfiedDependencyException unsatisfiedDependencyException) {
				return unsatisfiedDependencyException.getInjectionPoint();
			}
			return null;
		}

		/**
         * Compares this object to the specified object for equality.
         * 
         * @param obj the object to compare to
         * @return true if the objects are equal, false otherwise
         */
        @Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null || getClass() != obj.getClass()) {
				return false;
			}
			return this.name.equals(((BeanInCycle) obj).name);
		}

		/**
         * Returns the hash code value for the object based on the name property.
         *
         * @return the hash code value for the object
         */
        @Override
		public int hashCode() {
			return this.name.hashCode();
		}

		/**
         * Returns a string representation of the object.
         * 
         * @return the name and description of the object
         */
        @Override
		public String toString() {
			return this.name + this.description;
		}

		/**
         * Returns the BeanInCycle object associated with the given Throwable object.
         * 
         * @param ex the Throwable object to check
         * @return the BeanInCycle object associated with the Throwable object, or null if not found
         */
        static BeanInCycle get(Throwable ex) {
			if (ex instanceof BeanCreationException beanCreationException) {
				return get(beanCreationException);
			}
			return null;
		}

		/**
         * Returns a BeanInCycle object based on the given BeanCreationException.
         * 
         * @param ex the BeanCreationException to be used
         * @return a BeanInCycle object if the BeanCreationException has a bean name, otherwise null
         */
        private static BeanInCycle get(BeanCreationException ex) {
			if (StringUtils.hasText(ex.getBeanName())) {
				return new BeanInCycle(ex);
			}
			return null;
		}

	}

}
