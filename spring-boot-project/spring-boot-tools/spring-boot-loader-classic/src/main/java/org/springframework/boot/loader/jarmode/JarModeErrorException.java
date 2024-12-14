/*
 * Copyright 2012-2024 the original author or authors.
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

/**
 * Simple {@link RuntimeException} used to fail the jar mode with a simple printed error
 * message.
 *
 * @author Phillip Webb
 * @since 3.3.7
 */
public class JarModeErrorException extends RuntimeException {

	public JarModeErrorException(String message) {
		super(message);
	}

	public JarModeErrorException(String message, Throwable cause) {
		super(message, cause);
	}

}
