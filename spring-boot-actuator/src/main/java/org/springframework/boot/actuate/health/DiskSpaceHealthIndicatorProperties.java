/*
 * Copyright 2012-2014 the original author or authors.
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

import org.springframework.util.Assert;

/**
 * External configuration properties for {@link DiskSpaceHealthIndicator}
 *
 * @author Andy Wilkinson
 * @since 1.2.0
 */
public class DiskSpaceHealthIndicatorProperties {

	private File path = new File(".");

	private long threshold = 10 * 1024 * 1024;

	public File getPath() {
		return this.path;
	}

	public void setPath(File path) {
		if (!path.exists()) {
			throw new IllegalArgumentException(String.format("Path '%s' does not exist",
					path));
		}
		if (!path.canRead()) {
			throw new IllegalStateException(String.format("Path '%s' cannot be read",
					path));
		}
		this.path = path;
	}

	public long getThreshold() {

		return this.threshold;
	}

	public void setThreshold(long threshold) {
		Assert.isTrue(threshold >= 0, "threshold must be greater than 0");
		this.threshold = threshold;
	}
}
