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

package org.springframework.boot.actuate.autoconfigure.metrics.export.health;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@link ConfigurationProperties @ConfigurationProperties} for configuring health
 * indicators based on service level objectives.
 *
 * @author Jon Schneider
 * @since 2.4.0
 */
@ConfigurationProperties(prefix = "management.metrics.export.health")
public class HealthProperties {

	/**
	 * Step size (i.e. polling frequency for moving window indicators) to use.
	 */
	private Duration step = Duration.ofSeconds(10);

	/**
	 * Error budgets by API endpoint prefix. The value is a percentage in the range [0,1].
	 */
	private final Map<String, Double> apiErrorBudgets = new LinkedHashMap<>();

	public Duration getStep() {
		return this.step;
	}

	public void setStep(Duration step) {
		this.step = step;
	}

	public Map<String, Double> getApiErrorBudgets() {
		return this.apiErrorBudgets;
	}

}
