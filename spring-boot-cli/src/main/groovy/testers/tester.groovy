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


/**
 * Groovy script define abstract basis for automated testers for {@link TestCommand}.
 * Needs to be compiled along with the actual code to work properly.
 *
 * @author Greg Turnquist
 */
public abstract class AbstractTester {

    public TestResults findAndTest(List<Class<?>> compiled) throws FileNotFoundException {
        Set<Class<?>> testable = findTestableClasses(compiled)

        if (testable.size() == 0) {
            return TestResults.NONE
        }

        return test(testable.toArray(new Class<?>[0]))
    }

    protected abstract Set<Class<?>> findTestableClasses(List<Class<?>> compiled)

    protected abstract TestResults test(Class<?>[] testable)

}
