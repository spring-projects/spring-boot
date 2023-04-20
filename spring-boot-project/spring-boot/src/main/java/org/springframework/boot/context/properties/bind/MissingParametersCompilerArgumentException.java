/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.context.properties.bind;

import java.util.Set;

/**
 * Exception thrown to indicate that a class has not been compiled with
 * {@code -parameters}.
 *
 * @author Andy Wilkinson
 */
class MissingParametersCompilerArgumentException extends RuntimeException {

	MissingParametersCompilerArgumentException(Set<Class<?>> faultyClasses) {
		super(message(faultyClasses));
	}

	private static String message(Set<Class<?>> faultyClasses) {
		StringBuilder message = new StringBuilder(String.format(
				"Constructor binding in a native image requires compilation with -parameters but the following classes were compiled without it:%n"));
		for (Class<?> faultyClass : faultyClasses) {
			message.append(String.format("\t%s%n", faultyClass.getName()));
		}
		return message.toString();
	}

}
