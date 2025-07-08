/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.data.autoconfigure.metrics;

import io.micrometer.core.instrument.Timer.Builder;

import org.springframework.boot.data.autoconfigure.metrics.DataMetricsProperties.Repository.Autotime;
import org.springframework.boot.data.metrics.AutoTimer;

/**
 * {@link AutoTimer} whose behavior is configured by {@link Autotime} properties.
 *
 * @author Andy Wilkinson
 * @since 4.0.0
 */
public class PropertiesAutoTimer implements AutoTimer {

	private final Autotime properties;

	/**
	 * Create a new {@link PropertiesAutoTimer} configured using the given
	 * {@code properties}.
	 * @param properties the properties to configure auto-timing
	 */
	public PropertiesAutoTimer(Autotime properties) {
		this.properties = properties;
	}

	@Override
	public void apply(Builder builder) {
		builder.publishPercentileHistogram(this.properties.isPercentilesHistogram())
			.publishPercentiles(this.properties.getPercentiles());
	}

	@Override
	public boolean isEnabled() {
		return this.properties.isEnabled();
	}

}
