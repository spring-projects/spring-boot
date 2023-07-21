/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.autoconfigure.context;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for lifecycle processing.
 *
 * @author Andy Wilkinson
 * @since 2.3.0
 */
@ConfigurationProperties(prefix = "spring.lifecycle")
public class LifecycleProperties {

	/**
	 * Timeout for the shutdown of any phase (group of SmartLifecycle beans with the same
	 * 'phase' value).
	 */
	private Duration timeoutPerShutdownPhase = Duration.ofSeconds(30);

	public Duration getTimeoutPerShutdownPhase() {
		return this.timeoutPerShutdownPhase;
	}

	public void setTimeoutPerShutdownPhase(Duration timeoutPerShutdownPhase) {
		this.timeoutPerShutdownPhase = timeoutPerShutdownPhase;
	}

}
