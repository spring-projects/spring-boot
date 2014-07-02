/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.groovy;

import org.junit.internal.TextListener;
import org.junit.runner.JUnitCore;
import org.springframework.boot.cli.command.test.TestRunner;

/**
 * Delegate test runner to launch tests in user application classpath.
 *
 * @author Phillip Webb
 * @see TestRunner
 */
public class DelegateTestRunner {

	public static void run(Class<?>[] testClasses) {
		JUnitCore jUnitCore = new JUnitCore();
		jUnitCore.addListener(new TextListener(System.out));
		jUnitCore.run(testClasses);
	}

}
