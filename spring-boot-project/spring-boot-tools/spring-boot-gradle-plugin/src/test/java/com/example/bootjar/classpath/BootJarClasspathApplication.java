/*
 * Copyright 2012-2021 the original author or authors.
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

package com.example.bootjar.classpath;

import java.net.URL;
import java.net.URLClassLoader;

/**
 * Application used for testing classpath handling with BootJar.
 *
 * @author Andy Wilkinson
 */
public class BootJarClasspathApplication {

	protected BootJarClasspathApplication() {

	}

	public static void main(String[] args) {
		int i = 1;
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		for (URL url : ((URLClassLoader) classLoader).getURLs()) {
			System.out.println(i++ + ". " + url.getFile());
		}
	}

}
