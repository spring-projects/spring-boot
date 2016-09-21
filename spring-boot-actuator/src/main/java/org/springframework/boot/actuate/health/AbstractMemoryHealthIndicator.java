/*
 * Copyright 2016-2016 the original author or authors.
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

import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Base {@link HealthIndicator} implementation for all memory usage implementations.
 *
 * @author Ben Hale
 * @since 1.5.0
 */
public abstract class AbstractMemoryHealthIndicator extends AbstractHealthIndicator {

	private static final long UNDEFINED = -1;

	private static final Log logger = LogFactory.getLog(HeapMemoryHealthIndicator.class);

	private final AbstractMemoryHealthIndicatorProperties properties;

	/**
	 * Create a new {@link AbstractMemoryHealthIndicator}.
	 * @param properties the memory properties
	 */
	protected AbstractMemoryHealthIndicator(AbstractMemoryHealthIndicatorProperties properties) {
		this.properties = properties;
	}

	@Override
	protected final void doHealthCheck(Health.Builder builder) throws Exception {
		MemoryUsage memoryUsage = getMemoryUsage(this.properties.getMemoryMXBean());
		long available = memoryUsage.getMax() - memoryUsage.getUsed();
		long threshold = getThreshold(memoryUsage, this.properties.getThreshold());

		if (UNDEFINED == threshold || available > threshold) {
			builder.up();
		}
		else {
			logger.warn(String.format(
					"Free memory below threshold. "
						+ "Available: %d bytes (threshold: %d bytes)",
					available, threshold));
			builder.down();
		}

		builder.withDetail("init", memoryUsage.getInit())
			.withDetail("used", memoryUsage.getUsed())
			.withDetail("committed", memoryUsage.getCommitted())
			.withDetail("max", memoryUsage.getMax())
			.withDetail("threshold", threshold);
	}

	/**
	 * Returns the {@link MemoryUsage} to perform the health check on.
	 * @param memoryMXBean the configured {@link MemoryMXBean} to extract the {@code MemoryUsage} from
	 * @return the {@code MemoryUsage} to perform the health check on
	 */
	protected abstract MemoryUsage getMemoryUsage(MemoryMXBean memoryMXBean);

	private static long getThreshold(MemoryUsage memoryUsage, double threshold) {
		return UNDEFINED == memoryUsage.getMax() ? UNDEFINED : (long) (memoryUsage.getMax() * threshold);
	}

}
