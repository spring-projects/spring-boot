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

package org.springframework.boot.actuate.autoconfigure.metrics.jdbc;

import javax.sql.DataSource;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * ø {@link ConfigurationProperties @ConfigurationProperties} for configuring DataSource
 * pool metrics.
 *
 * @author Chris Bono
 * @since 2.5.0
 */
@ConfigurationProperties("management.metrics.jdbc")
public class DataSourcePoolMetricsProperties {

	/**
	 * Whether to ignore AbstractRoutingDataSources when creating datasource pool metrics.
	 */
	private boolean ignoreRoutingDataSources = false;

	/**
	 * Whether to ignore AbstractRoutingDataSources when the target data source is also a
	 * registered {@link DataSource} and therefore already has metrics auto configured for
	 * it.
	 */
	private boolean deduplicateRoutingDataSources = false;

	public boolean isIgnoreRoutingDataSources() {
		return this.ignoreRoutingDataSources;
	}

	public void setIgnoreRoutingDataSources(boolean ignoreRoutingDataSources) {
		this.ignoreRoutingDataSources = ignoreRoutingDataSources;
	}

	public boolean isDeduplicateRoutingDataSources() {
		return deduplicateRoutingDataSources;
	}

	public void setDeduplicateRoutingDataSources(boolean deduplicateRoutingDataSources) {
		this.deduplicateRoutingDataSources = deduplicateRoutingDataSources;
	}

}
