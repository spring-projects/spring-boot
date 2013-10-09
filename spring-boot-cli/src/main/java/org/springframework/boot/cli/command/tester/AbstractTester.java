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

import java.io.FileNotFoundException;
import java.util.List;
import java.util.Set;

/**
 * Abstract base class for tester implementations.
 * 
 * @author Greg Turnquist
 */
public abstract class AbstractTester {

	public TestResults findAndTest(List<Class<?>> compiled) throws FileNotFoundException {
		Set<Class<?>> testable = findTestableClasses(compiled);
		if (testable.size() == 0) {
			return TestResults.NONE;
		}
		return test(testable.toArray(new Class<?>[] {}));
	}

	protected abstract Set<Class<?>> findTestableClasses(List<Class<?>> compiled);

	protected abstract TestResults test(Class<?>[] testable);

}
