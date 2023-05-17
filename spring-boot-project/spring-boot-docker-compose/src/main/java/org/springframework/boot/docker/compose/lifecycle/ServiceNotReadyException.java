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

package org.springframework.boot.docker.compose.lifecycle;

import org.springframework.boot.docker.compose.core.RunningService;

/**
 * Exception thrown when a single {@link RunningService} is not ready.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class ServiceNotReadyException extends RuntimeException {

	private final RunningService service;

	ServiceNotReadyException(RunningService service, String message) {
		this(service, message, null);
	}

	ServiceNotReadyException(RunningService service, String message, Throwable cause) {
		super(message, cause);
		this.service = service;
	}

	RunningService getService() {
		return this.service;
	}

}
