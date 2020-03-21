/*
 * Copyright 2012-2020 the original author or authors.
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.gradle.BuildResult;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.tasks.testing.Test;
import org.gradle.api.tasks.testing.TestDescriptor;
import org.gradle.api.tasks.testing.TestListener;
import org.gradle.api.tasks.testing.TestResult;

/**
 * Plugin for recording test failures and reporting them at the end of the build.
 *
 * @author Andy Wilkinson
 */
public class TestFailuresPlugin implements Plugin<Project> {

	@Override
	public void apply(Project project) {
		TestResultsExtension testResults = getOrCreateTestResults(project);
		project.getTasks().withType(Test.class,
				(test) -> test.addTestListener(new FailureRecordingTestListener(testResults, test)));
	}

	private TestResultsExtension getOrCreateTestResults(Project project) {
		TestResultsExtension testResults = project.getRootProject().getExtensions()
				.findByType(TestResultsExtension.class);
		if (testResults == null) {
			testResults = project.getRootProject().getExtensions().create("testResults", TestResultsExtension.class);
			project.getRootProject().getGradle().buildFinished(testResults::buildFinished);
		}
		return testResults;
	}

	private final class FailureRecordingTestListener implements TestListener {

		private final List<TestFailure> failures = new ArrayList<>();

		private final TestResultsExtension testResults;

		private final Test test;

		private FailureRecordingTestListener(TestResultsExtension testResults, Test test) {
			this.testResults = testResults;
			this.test = test;
		}

		@Override
		public void afterSuite(TestDescriptor descriptor, TestResult result) {
			if (!this.failures.isEmpty()) {
				Collections.sort(this.failures);
				this.testResults.addFailures(this.test, this.failures);
			}
		}

		@Override
		public void afterTest(TestDescriptor descriptor, TestResult result) {
			if (result.getFailedTestCount() > 0) {
				this.failures.add(new TestFailure(descriptor));
			}
		}

		@Override
		public void beforeSuite(TestDescriptor descriptor) {

		}

		@Override
		public void beforeTest(TestDescriptor descriptor) {

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

	public static class TestResultsExtension {

		private final Map<Test, List<TestFailure>> testFailures = new TreeMap<>(
				(one, two) -> one.getPath().compareTo(two.getPath()));

		private final Object monitor = new Object();

		void addFailures(Test test, List<TestFailure> testFailures) {
			synchronized (this.monitor) {
				this.testFailures.put(test, testFailures);
			}
		}

		public void buildFinished(BuildResult result) {
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
					failures.forEach((failure) -> System.err.println(
							"    " + failure.descriptor.getClassName() + " > " + failure.descriptor.getName()));
				});
			}
		}

	}

}
