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

package org.springframework.boot;

/**
 * Interface used to generate an 'exit code' from a running command line
 * {@link SpringApplication}. Can be used on exceptions as well as directly on beans.
 *
 * @author Dave Syer
 * @see SpringApplication#exit(org.springframework.context.ApplicationContext,
 * ExitCodeGenerator...)
 */
@FunctionalInterface
public interface ExitCodeGenerator {

	/**
	 * Returns the exit code that should be returned from the application.
	 * @return the exit code.
	 */
	int getExitCode();

}
