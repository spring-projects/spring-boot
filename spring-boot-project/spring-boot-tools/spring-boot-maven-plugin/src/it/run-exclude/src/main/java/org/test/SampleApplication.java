/*
 * Copyright 2012-2017 the original author or authors.
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

package org.test;

public class SampleApplication {

	public static void main(String[] args) {
		if (isClassPresent("org.apache.log4j.Logger")) {
			throw new IllegalStateException("Log4j was present and should not");
		}
		if (isClassPresent("javax.servlet.Servlet")) {
			throw new IllegalStateException("servlet-api was present and should not");
		}
		System.out.println("I haz been run");
	}

	private static boolean isClassPresent(String className) {

		try {
			ClassLoader classLoader = SampleApplication.class.getClassLoader();
			classLoader.loadClass(className);
			return true;
		}
		catch (ClassNotFoundException e) {
			return false;
		}
	}

}
