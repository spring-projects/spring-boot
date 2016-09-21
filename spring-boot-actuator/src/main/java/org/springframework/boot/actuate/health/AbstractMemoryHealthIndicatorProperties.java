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

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;

import org.springframework.util.Assert;

/**
 * Base properties for all memory-reporting {@link HealthIndicator}s.
 *
 * @author Ben Hale
 * @since 1.5.0
 */
public abstract class AbstractMemoryHealthIndicatorProperties {

	private static final double DEFAULT_THRESHOLD = .05;
	/**
	 * The {@link MemoryMXBean} used to compute the available memory.
	 */
	private MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();

	/**
	 * Minimum memory space that should be available, as a percentage.
	 */
	private double threshold = DEFAULT_THRESHOLD;

	public final MemoryMXBean getMemoryMXBean() {
		return this.memoryMXBean;
	}

	public final void setMemoryMXBean(MemoryMXBean memoryMXBean) {
		Assert.notNull(memoryMXBean, "MemoryMXBean must not be null");
		this.memoryMXBean = memoryMXBean;
	}

	public final double getThreshold() {
		return this.threshold;
	}

	public final void setThreshold(double threshold) {
		Assert.isTrue(threshold >= 0, "threshold must be greater than or equal to 0");
		this.threshold = threshold;
	}

}
