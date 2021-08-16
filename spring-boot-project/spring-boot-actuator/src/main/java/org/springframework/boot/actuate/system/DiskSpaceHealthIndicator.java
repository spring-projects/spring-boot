/*
 * Copyright 2012-2021 the original author or authors.
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
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.core.log.LogMessage;
import org.springframework.util.unit.DataSize;

/**
 * A {@link HealthIndicator} that checks one or more paths for available disk space and
 * reports a status of {@link Status#DOWN} when any of the paths drops below a
 * configurable threshold.
 *
 * @author Mattias Severson
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @author Chris Bono
 * @since 2.0.0
 */
public class DiskSpaceHealthIndicator extends AbstractHealthIndicator {

	private static final Log logger = LogFactory.getLog(DiskSpaceHealthIndicator.class);

	private final Map<File, DataSize> paths = new LinkedHashMap<>();

	/**
	 * Create a new {@code DiskSpaceHealthIndicator} instance for a single path.
	 * @param path the Path used to compute the available disk space
	 * @param threshold the minimum disk space that should be available
	 */
	public DiskSpaceHealthIndicator(File path, DataSize threshold) {
		super("DiskSpace health check failed");
		this.paths.put(path, threshold);
	}

	/**
	 * Create a new {@code DiskSpaceHealthIndicator} instance for one or more paths.
	 * @param paths the paths to compute available disk space for and their corresponding
	 * minimum disk space that should be available.
	 */
	public DiskSpaceHealthIndicator(Map<File, DataSize> paths) {
		super("DiskSpace health check failed");
		this.paths.putAll(paths);
	}

	@Override
	protected void doHealthCheck(Health.Builder builder) throws Exception {
		// assume all is well - prove otherwise when checking paths
		builder.up();

		Map<String, Map<String, Object>> details = new LinkedHashMap<>();
		this.paths.forEach((path, threshold) -> details.put(path.getAbsolutePath(),
				checkPathAndGetDetails(path, threshold, builder)));
		builder.withDetail("paths", details);
	}

	private Map<String, Object> checkPathAndGetDetails(File path, DataSize threshold, Health.Builder builder) {
		long diskFreeInBytes = path.getUsableSpace();
		if (diskFreeInBytes < threshold.toBytes()) {
			logger.warn(LogMessage.format("Free disk space in %s below threshold. Available: %d bytes (threshold: %s)",
					path.getAbsolutePath(), diskFreeInBytes, threshold));
			builder.down();
		}
		Map<String, Object> pathDetails = new LinkedHashMap<>();
		pathDetails.put("total", path.getTotalSpace());
		pathDetails.put("free", diskFreeInBytes);
		pathDetails.put("threshold", threshold.toBytes());
		pathDetails.put("exists", path.exists());
		return pathDetails;
	}

}
