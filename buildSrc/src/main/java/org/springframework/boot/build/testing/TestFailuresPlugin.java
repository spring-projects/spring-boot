/*
 * Copyright 2012-present the original author or authors.
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

	@Override
	public void apply(Project project) {
		Provider<TestResultsOverview> testResultsOverview = project.getGradle()
			.getSharedServices()
			.registerIfAbsent("testResultsOverview", TestResultsOverview.class, (spec) -> {
			});
		project.getTasks().withType(Test.class, (test) -> {
			test.usesService(testResultsOverview);
			test.addTestListener(new FailureRecordingTestListener(testResultsOverview, test));
		});
	}

	private final class FailureRecordingTestListener implements TestListener {

		private final List<TestDescriptor> failures = new ArrayList<>();

		private final Provider<TestResultsOverview> testResultsOverview;

		private final Test test;

		private FailureRecordingTestListener(Provider<TestResultsOverview> testResultOverview, Test test) {
			this.testResultsOverview = testResultOverview;
			this.test = test;
		}

		@Override
		public void afterSuite(TestDescriptor descriptor, TestResult result) {
			if (!this.failures.isEmpty()) {
				this.testResultsOverview.get().addFailures(this.test, this.failures);
			}
		}

		@Override
		public void afterTest(TestDescriptor descriptor, TestResult result) {
			if (result.getFailedTestCount() > 0) {
				this.failures.add(descriptor);
			}
		}

		@Override
		public void beforeSuite(TestDescriptor descriptor) {

		}

		@Override
		public void beforeTest(TestDescriptor descriptor) {

		}

	}

}
