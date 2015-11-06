/*
 * Copyright 2014-2015 the original author or authors.
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

import java.io.File;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.annotation.Autowired;

/**
 * A {@link HealthIndicator} that checks available disk space and reports a status of
 * {@link Status#DOWN} when it drops below a configurable threshold.
 *
 * @author Mattias Severson
 * @author Andy Wilkinson
 * @since 1.2.0
 */
public class DiskSpaceHealthIndicator extends AbstractHealthIndicator {

	private static Log logger = LogFactory.getLog(DiskSpaceHealthIndicator.class);

	private final DiskSpaceHealthIndicatorProperties properties;

	/**
	 * Create a new {@code DiskSpaceHealthIndicator}.
	 * @param properties the disk space properties
	 */
	@Autowired
	public DiskSpaceHealthIndicator(DiskSpaceHealthIndicatorProperties properties) {
		this.properties = properties;
	}

	@Override
	protected void doHealthCheck(Health.Builder builder) throws Exception {
		File path = this.properties.getPath();
		long diskFreeInBytes = path.getFreeSpace();
		if (diskFreeInBytes >= this.properties.getThreshold()) {
			builder.up();
		}
		else {
			logger.warn(String.format(
					"Free disk space below threshold. "
							+ "Available: %d bytes (threshold: %d bytes)",
					diskFreeInBytes, this.properties.getThreshold()));
			builder.down();
		}
		builder.withDetail("total", path.getTotalSpace())
				.withDetail("free", diskFreeInBytes)
				.withDetail("threshold", this.properties.getThreshold());
	}
}
