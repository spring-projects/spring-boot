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

package org.springframework.boot.actuate.system;

import java.io.File;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;

/**
 * A {@link HealthIndicator} that checks available disk space and reports a status of
 * {@link Status#DOWN} when it drops below a configurable threshold.
 *
 * @author Mattias Severson
 * @author Andy Wilkinson
 * @since 2.0.0
 */
public class DiskSpaceHealthIndicator extends AbstractHealthIndicator {

	private static final Log logger = LogFactory.getLog(DiskSpaceHealthIndicator.class);

	private final File path;

	private final long threshold;

	/**
	 * Create a new {@code DiskSpaceHealthIndicator} instance.
	 * @param path the Path used to compute the available disk space
	 * @param threshold the minimum disk space that should be available (in bytes)
	 */
	public DiskSpaceHealthIndicator(File path, long threshold) {
		super("DiskSpace health check failed");
		this.path = path;
		this.threshold = threshold;
	}

	@Override
	protected void doHealthCheck(Health.Builder builder) throws Exception {
		long diskFreeInBytes = this.path.getUsableSpace();
		if (diskFreeInBytes >= this.threshold) {
			builder.up();
		}
		else {
			logger.warn(String.format("Free disk space below threshold. " + "Available: %d bytes (threshold: %d bytes)",
					diskFreeInBytes, this.threshold));
			builder.down();
		}
		builder.withDetail("total", this.path.getTotalSpace()).withDetail("free", diskFreeInBytes)
				.withDetail("threshold", this.threshold);
	}

}
