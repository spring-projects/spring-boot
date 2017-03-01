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

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.Assert;

/**
 * External configuration properties for {@link MemoryHealthIndicator}.
 *
 * @author Lari Hotari
 */
@ConfigurationProperties(prefix = "management.health.memory")
public class MemoryHealthIndicatorProperties {
	private static final int DEFAULT_THRESHOLD = 90;

	/**
	 * Maximum tenured heap space usage, in percentage.
	 */
	private int occupiedHeapPercentageThreshold = DEFAULT_THRESHOLD;

	public int getThreshold() {
		return this.occupiedHeapPercentageThreshold;
	}

	public void setThreshold(int occupiedHeapPercentageThreshold) {
		Assert.isTrue(occupiedHeapPercentageThreshold >= 0,
				"threshold must be greater than 0");
		this.occupiedHeapPercentageThreshold = occupiedHeapPercentageThreshold;
	}
}
