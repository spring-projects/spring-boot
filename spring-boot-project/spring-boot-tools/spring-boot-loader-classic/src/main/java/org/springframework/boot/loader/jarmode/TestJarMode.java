/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.loader.jarmode;

import java.util.Arrays;

/**
 * {@link JarMode} for testing.
 *
 * @author Phillip Webb
 */
class TestJarMode implements JarMode {

	/**
	 * Determines if the given mode is accepted.
	 * @param mode the mode to be checked
	 * @return true if the mode is accepted, false otherwise
	 */
	@Override
	public boolean accepts(String mode) {
		return "test".equals(mode);
	}

	/**
	 * Runs the program in the specified mode with the given arguments.
	 * @param mode the mode in which the program should run
	 * @param args the arguments to be passed to the program
	 */
	@Override
	public void run(String mode, String[] args) {
		System.out.println("running in " + mode + " jar mode " + Arrays.asList(args));
	}

}
