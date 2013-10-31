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

import org.springframework.boot.cli.command.tester.TestResults
import spock.lang.Specification

/**
 * Groovy script to run Spock tests inside the {@link TestCommand}.
 * Needs to be compiled along with the actual code to work properly.
 *
 * NOTE: SpockTester depends on JUnitTester to actually run the tests
 *
 * @author Greg Turnquist
 */
class SpockTester extends AbstractTester {

	@Override
	protected Set<Class<?>> findTestableClasses(List<Class<?>> compiled) {
		// Look for classes that implement spock.lang.Specification
		Set<Class<?>> testable = new LinkedHashSet<Class<?>>()
		for (Class<?> clazz : compiled) {
			if (Specification.class.isAssignableFrom(clazz)) {
				testable.add(clazz)
			}
		}
		return testable
	}

	@Override
	protected TestResults test(List<Class<?>> testable) {
		return JUnitTester.runEmbeddedTests(testable)
	}

}
