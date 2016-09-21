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

/**
 * A {@link HealthIndicator} that checks available non-heap memory and reports a status
 * of {@link Status#DOWN} when either drops below a configurable threshold.
 *
 * @author Ben Hale
 * @since 1.5.0
 */
public class NonHeapMemoryHealthIndicator extends AbstractMemoryHealthIndicator {

	/**
	 * Create a new {@code NonHeapMemoryHealthIndicator}.
	 * @param properties the memory properties
	 */
	public NonHeapMemoryHealthIndicator(NonHeapMemoryHealthIndicatorProperties properties) {
		super(properties);
	}

	@Override
	protected MemoryUsage getMemoryUsage(MemoryMXBean memoryMXBean) {
		return memoryMXBean.getNonHeapMemoryUsage();
	}

}
