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

import org.junit.runner.Result
import org.springframework.boot.cli.command.tester.Failure
import org.springframework.boot.cli.command.tester.TestResults
import spock.lang.Specification
import spock.util.EmbeddedSpecRunner

/**
 * Groovy script to run Spock tests inside the {@link TestCommand}.
 * Needs to be compiled along with the actual code to work properly.
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
    protected TestResults test(Class<?>[] testable) {
        Result results = new EmbeddedSpecRunner().runClasses(Arrays.asList(testable))

        TestResults testResults = new TestResults()
        testResults.setFailureCount(results.getFailureCount())
        testResults.setRunCount(results.getRunCount())

        List<org.springframework.boot.cli.command.tester.Failure> failures =
            new ArrayList<Failure>()
        for (org.junit.runner.notification.Failure failure : results.getFailures()) {
            failures.add(new Failure(failure.getDescription().toString(), failure.getTrace()))
        }

        testResults.setFailures(failures.toArray(new Failure[0]))

        return testResults
    }

}
