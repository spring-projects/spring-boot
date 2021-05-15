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

package org.springframework.boot.buildpack.platform.docker;

import org.springframework.util.Assert;

/**
 * Event published by the {@link TotalProgressPullListener} showing the total progress of
 * an operation.
 *
 * @author Phillip Webb
 * @since 2.3.0
 */
public class TotalProgressEvent {

	private final int percent;

	/**
	 * Create a new {@link TotalProgressEvent} with a specific percent value.
	 * @param percent the progress as a percentage
	 */
	public TotalProgressEvent(int percent) {
		Assert.isTrue(percent >= 0 && percent <= 100, "Percent must be in the range 0 to 100");
		this.percent = percent;
	}

	/**
	 * Return the total progress.
	 * @return the total progress
	 */
	public int getPercent() {
		return this.percent;
	}

}
