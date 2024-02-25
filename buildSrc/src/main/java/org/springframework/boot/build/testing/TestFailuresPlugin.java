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

import java.util.ArrayList;
import java.util.List;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.provider.Provider;
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

	/**
     * Applies the TestFailuresPlugin to the given project.
     * 
     * @param project the project to apply the plugin to
     */
    @Override
	public void apply(Project project) {
		Provider<TestResultsOverview> testResultsOverview = project.getGradle()
			.getSharedServices()
			.registerIfAbsent("testResultsOverview", TestResultsOverview.class, (spec) -> {
			});
		project.getTasks()
			.withType(Test.class,
					(test) -> test.addTestListener(new FailureRecordingTestListener(testResultsOverview, test)));
	}

	/**
     * FailureRecordingTestListener class.
     */
    private final class FailureRecordingTestListener implements TestListener {

		private final List<TestDescriptor> failures = new ArrayList<>();

		private final Provider<TestResultsOverview> testResultsOverview;

		private final Test test;

		/**
         * Constructs a new FailureRecordingTestListener with the specified test result overview provider and test.
         * 
         * @param testResultOverview the provider for the test results overview
         * @param test the test to be recorded
         */
        private FailureRecordingTestListener(Provider<TestResultsOverview> testResultOverview, Test test) {
			this.testResultsOverview = testResultOverview;
			this.test = test;
		}

		/**
         * This method is called after the execution of a test suite.
         * It checks if there are any failures recorded during the test suite execution.
         * If there are failures, it adds them to the test results overview.
         * 
         * @param descriptor The test descriptor of the executed test suite.
         * @param result The test result of the executed test suite.
         */
        @Override
		public void afterSuite(TestDescriptor descriptor, TestResult result) {
			if (!this.failures.isEmpty()) {
				this.testResultsOverview.get().addFailures(this.test, this.failures);
			}
		}

		/**
         * This method is called after each test execution to record any failures.
         * If the test result has any failed tests, the descriptor of the failed test is added to the list of failures.
         *
         * @param descriptor The descriptor of the test that was executed.
         * @param result The result of the test execution.
         */
        @Override
		public void afterTest(TestDescriptor descriptor, TestResult result) {
			if (result.getFailedTestCount() > 0) {
				this.failures.add(descriptor);
			}
		}

		/**
         * This method is called before the execution of a test suite.
         * 
         * @param descriptor the TestDescriptor object representing the test suite
         */
        @Override
		public void beforeSuite(TestDescriptor descriptor) {

		}

		/**
         * This method is called before each test execution.
         * 
         * @param descriptor the test descriptor containing information about the test
         */
        @Override
		public void beforeTest(TestDescriptor descriptor) {

		}

	}

}
