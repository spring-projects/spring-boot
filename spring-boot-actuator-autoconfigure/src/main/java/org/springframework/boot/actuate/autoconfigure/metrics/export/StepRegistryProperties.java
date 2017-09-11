/*
 * Copyright 2012-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.actuate.autoconfigure.metrics.export;

import java.time.Duration;

import io.micrometer.core.instrument.spectator.step.StepRegistryConfig;

/**
 * Specialization of {@link RegistryProperties} for configuring a metrics registry that
 * pushes aggregated metrics on a regular interval.
 *
 * @author Jon Schneider
 * @author Andy Wilkinson
 * @since 2.0.0
 */
public abstract class StepRegistryProperties extends RegistryProperties
		implements StepRegistryConfig {

	public void setStep(Duration step) {
		set("step", step);
	}

	public void setEnabled(Boolean enabled) {
		set("enabled", enabled);
	}

	public void setBatchSize(Integer batchSize) {
		set("batchSize", batchSize);
	}

	public void setConnectTimeout(Duration connectTimeout) {
		set("connectTimeout", connectTimeout);
	}

	public void setReadTimeout(Duration readTimeout) {
		set("readTimeout", readTimeout);
	}

	public void setNumThreads(Integer numThreads) {
		set("numThreads", numThreads);
	}

}
