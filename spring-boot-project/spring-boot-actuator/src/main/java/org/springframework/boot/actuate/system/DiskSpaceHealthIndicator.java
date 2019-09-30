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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.util.CollectionUtils;
import org.springframework.util.unit.DataSize;

/**
 * A {@link HealthIndicator} that checks available disk space and reports a status of
 * {@link Status#DOWN} when it drops below a configurable threshold.
 *
 * @author Mattias Severson
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @author Leo Li
 * @since 2.0.0
 */
public class DiskSpaceHealthIndicator extends AbstractHealthIndicator {

	private static final Log logger = LogFactory.getLog(DiskSpaceHealthIndicator.class);

	private final List<File> path;

	private final DataSize threshold;

	/**
	 * Create a new {@code DiskSpaceHealthIndicator} instance.
	 * @param path the Path used to compute the available disk space
	 * @param threshold the minimum disk space that should be available
	 */
	public DiskSpaceHealthIndicator(List<File> path, DataSize threshold) {
		super("DiskSpace health check failed");
		this.path = path;
		this.threshold = threshold;
	}

	@Override
	protected void doHealthCheck(Health.Builder builder) throws Exception {
		boolean status = true;
		Map<File, Long> diskFreeInBytesMap = new LinkedHashMap<>();
		for (File file : this.path) {
			long diskFreeInBytes = file.getUsableSpace();
			diskFreeInBytesMap.put(file, diskFreeInBytes);
			if (status && diskFreeInBytes < this.threshold.toBytes()) {
				logger.warn(String.format("Free disk space in %s below threshold. Available: %d bytes (threshold: %s)",
						file.getPath(), diskFreeInBytes, this.threshold));
				builder.down();
				status = false;
			}
		}
		if (status) {
			builder.up();
		}
		Map<String, Map<String, Long>> details = new LinkedHashMap<>();
		diskFreeInBytesMap.forEach((file, diskFreeInBytes) -> {
			if (".".equals(file.getPath())) {
				builder.withDetail("total", file.getTotalSpace()).withDetail("free", diskFreeInBytes)
						.withDetail("threshold", this.threshold.toBytes());
			}
			else {
				Map<String, Long> detail = new LinkedHashMap<>();
				detail.put("total", file.getTotalSpace());
				detail.put("free", diskFreeInBytes);
				details.put(file.getPath(), detail);
			}
		});
		if (!CollectionUtils.isEmpty(details)) {
			builder.withDetail("paths", details);
		}
	}

}
