/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.system;

import java.io.File;

import org.springframework.boot.actuate.system.DiskSpaceHealthIndicator;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.Assert;
import org.springframework.util.unit.DataSize;

/**
 * External configuration properties for {@link DiskSpaceHealthIndicator}.
 *
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @since 1.2.0
 */
@ConfigurationProperties(prefix = "management.health.diskspace")
public class DiskSpaceHealthIndicatorProperties {

	/**
	 * Path used to compute the available disk space.
	 */
	private File path = new File(".");

	/**
	 * Minimum disk space that should be available.
	 */
	private DataSize threshold = DataSize.ofMegabytes(10);

	/**
	 * Returns the path of the file.
	 * @return the path of the file
	 */
	public File getPath() {
		return this.path;
	}

	/**
	 * Sets the path for the DiskSpaceHealthIndicatorProperties.
	 * @param path the file path to set
	 */
	public void setPath(File path) {
		this.path = path;
	}

	/**
	 * Returns the threshold value for disk space.
	 * @return the threshold value for disk space
	 */
	public DataSize getThreshold() {
		return this.threshold;
	}

	/**
	 * Sets the threshold for disk space health indicator.
	 * @param threshold the threshold value to be set
	 * @throws IllegalArgumentException if the threshold is negative
	 */
	public void setThreshold(DataSize threshold) {
		Assert.isTrue(!threshold.isNegative(), "threshold must be greater than or equal to 0");
		this.threshold = threshold;
	}

}
