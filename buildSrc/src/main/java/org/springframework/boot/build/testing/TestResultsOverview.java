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

package org.springframework.boot.build.testing;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

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

	private final Map<Test, List<TestFailure>> testFailures = new TreeMap<>(
			(one, two) -> one.getPath().compareTo(two.getPath()));

	private final Object monitor = new Object();

	void addFailures(Test test, List<TestDescriptor> failureDescriptors) {
		List<TestFailure> testFailures = failureDescriptors.stream().map(TestFailure::new).sorted()
				.collect(Collectors.toList());
		synchronized (this.monitor) {
			this.testFailures.put(test, testFailures);
		}
	}

	@Override
	public void onFinish(FinishEvent event) {
		// OperationCompletionListener is implemented to defer close until the build ends
	}

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

	private static final class TestFailure implements Comparable<TestFailure> {

		private final TestDescriptor descriptor;

		private TestFailure(TestDescriptor descriptor) {
			this.descriptor = descriptor;
		}

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
