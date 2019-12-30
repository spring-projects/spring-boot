/*
 * Copyright 2012-2019 the original author or authors.
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

import java.util.Arrays;

public class SampleApplication {

	public static void main(String[] args) {
		if (args.length < 2) {
			throw new IllegalArgumentException("Missing arguments " + Arrays.toString(args) + "");
		}
		if (!args[0].startsWith("--management.endpoints.web.exposure.include=")) {
			throw new IllegalArgumentException("Invalid argument " + args[0]);
		}
		if (!args[1].startsWith("--spring.profiles.active=")) {
			throw new IllegalArgumentException("Invalid argument " + args[1]);
		}
		String endpoints = args[0].split("=")[1];
		String profile = args[1].split("=")[1];
		System.out.println("I haz been run with profile(s) '" + profile + "' and endpoint(s) '" + endpoints + "'");
	}

}
