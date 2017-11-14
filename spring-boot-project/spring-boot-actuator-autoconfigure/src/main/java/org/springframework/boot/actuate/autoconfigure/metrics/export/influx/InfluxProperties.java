/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.metrics.export.influx;

import io.micrometer.influx.InfluxConsistency;

import org.springframework.boot.actuate.autoconfigure.metrics.export.StepRegistryProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@link ConfigurationProperties} for configuring Influx metrics export.
 *
 * @author Jon Schneider
 * @since 2.0.0
 */
@ConfigurationProperties(prefix = "spring.metrics.export.influx")
public class InfluxProperties extends StepRegistryProperties {

	/**
	 * Tag that will be mapped to "host" when shipping metrics to Influx. Can be
	 * omitted of host should be omitted on publishing.
	 */
	private String db;

	/**
	 * Write consistency for each point.
	 */
	private InfluxConsistency consistency;

	/**
	 * Login user of the Influx server.
	 */
	private String userName;

	/**
	 * Login password of the Influx server.
	 */
	private String password;

	/**
	 * Retention policy to use (Influx writes to the DEFAULT retention policy if one is
	 * not specified).
	 */
	private String retentionPolicy;

	/**
	 * URI of the Influx server.
	 */
	private String uri;

	/**
	 * Enable GZIP compression of metrics batches published to Influx.
	 */
	private Boolean compressed;

	public String getDb() {
		return this.db;
	}

	public void setDb(String db) {
		this.db = db;
	}

	public InfluxConsistency getConsistency() {
		return this.consistency;
	}

	public void setConsistency(InfluxConsistency consistency) {
		this.consistency = consistency;
	}

	public String getUserName() {
		return this.userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getPassword() {
		return this.password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getRetentionPolicy() {
		return this.retentionPolicy;
	}

	public void setRetentionPolicy(String retentionPolicy) {
		this.retentionPolicy = retentionPolicy;
	}

	public String getUri() {
		return this.uri;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}

	public Boolean getCompressed() {
		return this.compressed;
	}

	public void setCompressed(Boolean compressed) {
		this.compressed = compressed;
	}

}
