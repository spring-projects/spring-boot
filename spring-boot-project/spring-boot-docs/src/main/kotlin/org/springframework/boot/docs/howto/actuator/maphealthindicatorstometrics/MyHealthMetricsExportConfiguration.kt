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

package org.springframework.boot.docs.howto.actuator.maphealthindicatorstometrics

import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.boot.actuate.health.HealthEndpoint
import org.springframework.boot.actuate.health.Status
import org.springframework.context.annotation.Configuration

@Configuration(proxyBeanMethods = false)
class MyHealthMetricsExportConfiguration(registry: MeterRegistry, healthEndpoint: HealthEndpoint) {

	init {
		// This example presumes common tags (such as the app) are applied elsewhere
		Gauge.builder("health", healthEndpoint) { health ->
			getStatusCode(health).toDouble()
		}.strongReference(true).register(registry)
	}

	private fun getStatusCode(health: HealthEndpoint): Int {
		val status = health.health().status
		if (Status.UP == status) {
			return 3
		}
		if (Status.OUT_OF_SERVICE == status) {
			return 2
		}
		if (Status.DOWN == status) {
			return 1
		}
		return 0
	}

}
