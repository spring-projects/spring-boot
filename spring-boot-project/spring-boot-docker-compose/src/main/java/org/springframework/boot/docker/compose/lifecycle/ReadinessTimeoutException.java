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

import java.time.Duration;
import java.util.List;
import java.util.Objects;

import org.springframework.boot.docker.compose.core.RunningService;

/**
 * Exception thrown if readiness checking has timed out.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @since 3.1.0
 */
public final class ReadinessTimeoutException extends RuntimeException {

	private final Duration timeout;

	ReadinessTimeoutException(Duration timeout, List<ServiceNotReadyException> exceptions) {
		super(buildMessage(timeout, exceptions));
		this.timeout = timeout;
		exceptions.forEach(this::addSuppressed);
	}

	private static String buildMessage(Duration timeout, List<ServiceNotReadyException> exceptions) {
		List<String> serviceNames = exceptions.stream()
			.map(ServiceNotReadyException::getService)
			.filter(Objects::nonNull)
			.map(RunningService::name)
			.toList();
		return "Readiness timeout of %s reached while waiting for services %s".formatted(timeout, serviceNames);
	}

	/**
	 * Return the timeout that was reached.
	 * @return the timeout
	 */
	public Duration getTimeout() {
		return this.timeout;
	}

}
