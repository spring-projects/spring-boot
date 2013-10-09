/*
 * Copyright 2012-2013 the original author or authors.
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

package org.springframework.boot.cli.command.tester;

import java.util.Arrays;
import java.util.List;

/**
 * Platform neutral way to collect test results
 * 
 * NOTE: This is needed to avoid having to add JUnit's jar file to the deployable
 * artifacts
 * 
 * @author Greg Turnquist
 */
public class TestResults {

	public static final TestResults NONE = new TestResults() {

		@Override
		public int getRunCount() {
			return 0;
		}

		@Override
		public int getFailureCount() {
			return 0;
		}

		@Override
		public Failure[] getFailures() {
			return new Failure[0];
		}

		@Override
		public boolean wasSuccessful() {
			return true;
		}

	};

	private int runCount;

	private int failureCount;

	private Failure[] failures = new Failure[0];

	public void add(TestResults results) {
		this.runCount += results.getRunCount();
		this.failureCount += results.getFailureCount();
		List<Failure> failures = Arrays.asList(this.failures);
		failures.addAll(Arrays.asList(results.getFailures()));
		this.failures = failures.toArray(new Failure[] {});
	}

	public boolean wasSuccessful() {
		return this.failureCount == 0;
	}

	public int getRunCount() {
		return this.runCount;
	}

	public void setRunCount(int runCount) {
		this.runCount = runCount;
	}

	public int getFailureCount() {
		return this.failureCount;
	}

	public void setFailureCount(int failureCount) {
		this.failureCount = failureCount;
	}

	public Failure[] getFailures() {
		return this.failures;
	}

	public void setFailures(Failure[] failures) {
		this.failures = failures;
	}

}
