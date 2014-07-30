/*
 * Copyright 2014 the original author or authors.
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

/**
 * A {@link HealthIndicator} that checks for available disk space.
 *
 * @author Mattias Severson
 * @since 1.2.0
 */
@Component
public class DiskSpaceHealthIndicator extends AbstractHealthIndicator {
	private static Log logger = LogFactory.getLog(DiskSpaceHealthIndicator.class);

	private final File path;
	private final long thresholdBytes;

	/**
	 * Constructor.
	 * @param path The path to a directory for checking available disk space. The
	 *                application must have read access to this path. Additionally, the
	 *                {@link java.lang.SecurityManager#checkRead(String)} will be called
	 *                if a security manager is used.
	 *                By default, it uses the system property {@code user.dir}, i.e. the
	 *                current directory when the JVM was started.
	 * @param thresholdBytes The threshold (in Bytes) of remaining disk space that will
	 *                          trigger this health indicator when exceeded.
	 *                          Default value is 10485760 Bytes (10 MB).
	 */
	@Autowired
	public DiskSpaceHealthIndicator(@Value("${health.path?:${user.dir}}") String path,
									@Value("${health.threshold.bytes:10485760}") long thresholdBytes) {
		this.path = new File(path);
		this.thresholdBytes = thresholdBytes;
		verifyPathIsAccessible(this.path);
		Assert.isTrue(thresholdBytes >= 0, "thresholdBytes must be greater than 0");
	}

	@Override
	protected void doHealthCheck(Health.Builder builder) throws Exception {
		long diskFreeInBytes = this.path.getFreeSpace();
		if (diskFreeInBytes >= this.thresholdBytes) {
			builder.up();
		} else {
			logger.warn(String.format("Free disk space threshold exceeded. " +
							"Available: %d bytes (threshold: %d bytes).",
					diskFreeInBytes, this.thresholdBytes));
			builder.down();
		}
	}

	private static void verifyPathIsAccessible(File path) {
		if (!path.exists()) {
			throw new IllegalArgumentException(String.format("Path does not exist: %s", path));
		}
		if (!path.canRead()) {
			throw new IllegalStateException(String.format("Path cannot be read: %s", path));
		}
	}
}
