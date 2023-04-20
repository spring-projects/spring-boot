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

package org.springframework.boot.docker.compose.core;

/**
 * {@link DockerException} thrown if the docker daemon is not running.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @since 3.1.0
 */
public class DockerNotRunningException extends DockerException {

	private final String errorOutput;

	DockerNotRunningException(String errorOutput, Throwable cause) {
		super("Docker is not running", cause);
		this.errorOutput = errorOutput;
	}

	/**
	 * Return the error output returned from docker.
	 * @return the error output
	 */
	public String getErrorOutput() {
		return this.errorOutput;
	}

}
