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

package org.springframework.boot.actuate.autoconfigure.metrics.export.simple;

import java.time.Duration;

import io.micrometer.core.instrument.simple.CountingMode;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@link ConfigurationProperties @ConfigurationProperties} for configuring metrics export
 * to a {@link SimpleMeterRegistry}.
 *
 * @author Jon Schneider
 * @author Stephane Nicoll
 * @since 2.0.0
 */
@ConfigurationProperties(prefix = "management.metrics.export.simple")
public class SimpleProperties {

	/**
	 * Step size (i.e. reporting frequency) to use.
	 */
	private Duration step = Duration.ofMinutes(1);

	/**
	 * Counting mode.
	 */
	private CountingMode mode = CountingMode.CUMULATIVE;

	public Duration getStep() {
		return this.step;
	}

	public void setStep(Duration step) {
		this.step = step;
	}

	public CountingMode getMode() {
		return this.mode;
	}

	public void setMode(CountingMode mode) {
		this.mode = mode;
	}

}
