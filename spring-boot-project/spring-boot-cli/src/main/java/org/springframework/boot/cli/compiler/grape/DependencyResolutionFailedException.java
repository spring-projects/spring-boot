/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.cli.compiler.grape;

/**
 * Thrown to indicate a failure during dependency resolution.
 *
 * @author Andy Wilkinson
 */
@SuppressWarnings("serial")
public class DependencyResolutionFailedException extends RuntimeException {

	/**
	 * Creates a new {@code DependencyResolutionFailedException} with the given
	 * {@code cause}.
	 * @param cause the cause of the resolution failure
	 */
	public DependencyResolutionFailedException(Throwable cause) {
		super(cause);
	}

}
