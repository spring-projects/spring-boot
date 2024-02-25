/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.build.testing;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.gradle.api.DefaultTask;
import org.gradle.api.services.BuildService;
import org.gradle.api.services.BuildServiceParameters;
import org.gradle.api.tasks.testing.Test;
import org.gradle.api.tasks.testing.TestDescriptor;
import org.gradle.tooling.events.FinishEvent;
import org.gradle.tooling.events.OperationCompletionListener;

/**
 * {@link BuildService} that provides an overview of all the test failures in the build.
 *
 * @author Andy Wilkinson
 */
public abstract class TestResultsOverview
		implements BuildService<BuildServiceParameters.None>, OperationCompletionListener, AutoCloseable {

	private final Map<Test, List<TestFailure>> testFailures = new TreeMap<>(Comparator.comparing(DefaultTask::getPath));

	private final Object monitor = new Object();

	/**
     * Adds the list of failure descriptors to the test results for a specific test.
     * 
     * @param test the test for which the failures occurred
     * @param failureDescriptors the list of failure descriptors to be added
     */
    void addFailures(Test test, List<TestDescriptor> failureDescriptors) {
		List<TestFailure> testFailures = failureDescriptors.stream().map(TestFailure::new).sorted().toList();
		synchronized (this.monitor) {
			this.testFailures.put(test, testFailures);
		}
	}

	/**
     * This method is called when the build finishes.
     * It implements the OperationCompletionListener interface to defer the close operation until the build ends.
     *
     * @param event the FinishEvent object representing the event of the build finishing
     */
    @Override
	public void onFinish(FinishEvent event) {
		// OperationCompletionListener is implemented to defer close until the build ends
	}

	/**
     * Closes the TestResultsOverview object.
     * 
     * This method is responsible for printing any test failures found during the execution of test tasks.
     * If no test failures are found, the method returns without performing any action.
     * 
     * The test failures are printed to the standard error stream in a formatted manner.
     * The number of test failures found is displayed along with the corresponding test task paths.
     * For each test task, the class name and name of each failure are printed.
     * 
     * This method is synchronized on the monitor object to ensure thread safety.
     * 
     * @see TestResultsOverview
     */
    @Override
	public void close() {
		synchronized (this.monitor) {
			if (this.testFailures.isEmpty()) {
				return;
			}
			System.err.println();
			System.err.println("Found test failures in " + this.testFailures.size() + " test task"
					+ ((this.testFailures.size() == 1) ? ":" : "s:"));
			this.testFailures.forEach((task, failures) -> {
				System.err.println();
				System.err.println(task.getPath());
				failures.forEach((failure) -> System.err
					.println("    " + failure.descriptor.getClassName() + " > " + failure.descriptor.getName()));
			});
		}
	}

	/**
     * TestFailure class.
     */
    private static final class TestFailure implements Comparable<TestFailure> {

		private final TestDescriptor descriptor;

		/**
         * Constructs a new TestFailure with the specified TestDescriptor.
         *
         * @param descriptor the TestDescriptor associated with the TestFailure
         */
        private TestFailure(TestDescriptor descriptor) {
			this.descriptor = descriptor;
		}

		/**
         * Compares this TestFailure object with the specified TestFailure object for order.
         * The comparison is based on the class name and then the method name of the TestFailure objects.
         * 
         * @param other the TestFailure object to be compared
         * @return a negative integer, zero, or a positive integer as this TestFailure object is less than, equal to, or greater than the specified TestFailure object
         */
        @Override
		public int compareTo(TestFailure other) {
			int comparison = this.descriptor.getClassName().compareTo(other.descriptor.getClassName());
			if (comparison == 0) {
				comparison = this.descriptor.getName().compareTo(other.descriptor.getName());
			}
			return comparison;
		}

	}

}
