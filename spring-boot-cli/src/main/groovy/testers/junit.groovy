/*
 * Copyright 2012-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
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

import org.junit.runner.JUnitCore
import org.junit.runner.Result
import org.springframework.boot.cli.command.tester.Failure
import org.springframework.boot.cli.command.tester.TestResults

import java.lang.annotation.Annotation
import java.lang.reflect.Method


/**
 * Groovy script to run JUnit tests inside the {@link TestCommand}.
 * Needs to be compiled along with the actual code to work properly.
 *
 * @author Greg Turnquist
 */
class JUnitTester extends AbstractTester {

	@Override
	protected Set<Class<?>> findTestableClasses(List<Class<?>> compiled) {
		// Look for @Test methods
		Set<Class<?>> testable = new LinkedHashSet<Class<?>>()
		for (Class<?> clazz : compiled) {
			for (Method method : clazz.getMethods()) {
				for (Annotation annotation : method.getAnnotations()) {
					if (annotation.toString().contains("Test")) {
						testable.add(clazz)
					}
				}
			}
		}
		return testable
	}

	@Override
	protected TestResults test(List<Class<?>> testable) {
		return JUnitTester.runEmbeddedTests(testable)
	}

	static TestResults runEmbeddedTests(List<Class<?>> testable) {
		Result results = JUnitCore.runClasses(testable.toArray(new Class<?>[0]))

		TestResults testResults = new TestResults()
		testResults.setRunCount(results.getRunCount())

		List<Failure> failures = new ArrayList<Failure>()
		for (org.junit.runner.notification.Failure failure : results.getFailures()) {
			failures.add(new Failure(failure.exception.toString(), failure.trace))
		}

		testResults.setFailures(failures)

		return testResults
	}

}
