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

package org.springframework.boot.actuate.health;

import java.io.Closeable;
import java.io.IOException;
import java.lang.management.MemoryUsage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A {@link HealthIndicator} that checks available memory in the tenured space and reports
 * a status of {@link Status#DOWN} when the memory usage grows over a configurable
 * threshold percentage.
 *
 * See {@link LowMemoryDetector}
 * @author Lari Hotari
 */
public class MemoryHealthIndicator extends AbstractHealthIndicator implements Closeable {

	private static final Log logger = LogFactory.getLog(MemoryHealthIndicator.class);

	private final LowMemoryDetector lowMemoryDetector;

	/**
	 * Create a new {@code MemoryHealthIndicator}.
	 *
	 * @param properties the settings for this indicator
	 */
	public MemoryHealthIndicator(MemoryHealthIndicatorProperties properties) {
		this(new LowMemoryDetector(properties.getThreshold()));
	}

	MemoryHealthIndicator(LowMemoryDetector lowMemoryDetector) {
		this.lowMemoryDetector = lowMemoryDetector;
	}

	@Override
	protected void doHealthCheck(Health.Builder builder) throws Exception {
		MemoryUsage memoryUsage;
		if (this.lowMemoryDetector.isHealthy()) {
			builder.up();
			memoryUsage = this.lowMemoryDetector.getCurrentUsage();
		}
		else {
			memoryUsage = this.lowMemoryDetector.getLowMemoryStateUsage();
			if (memoryUsage == null) {
				memoryUsage = this.lowMemoryDetector.getCurrentUsage();
			}
			logger.warn(String.format(
					"Tenured heap space usage over threshold. "
							+ "Usage: %d / %d bytes (threshold: %d%%)",
					memoryUsage.getUsed(), memoryUsage.getMax(),
					this.lowMemoryDetector.getOccupiedHeapPercentageThreshold()));
			builder.down();
		}
		builder.withDetail("used", memoryUsage.getUsed())
				.withDetail("max", memoryUsage.getMax()).withDetail("thresholdPercentage",
						this.lowMemoryDetector.getOccupiedHeapPercentageThreshold());
	}

	@Override
	public void close() throws IOException {
		this.lowMemoryDetector.stop();
	}
}
