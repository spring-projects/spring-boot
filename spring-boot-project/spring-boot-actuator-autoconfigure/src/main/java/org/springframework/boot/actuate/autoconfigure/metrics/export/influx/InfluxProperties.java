/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.metrics.export.influx;

import io.micrometer.influx.InfluxConsistency;

import org.springframework.boot.actuate.autoconfigure.metrics.export.properties.StepRegistryProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@link ConfigurationProperties} for configuring Influx metrics export.
 *
 * @author Jon Schneider
 * @author Stephane Nicoll
 * @since 2.0.0
 */
@ConfigurationProperties(prefix = "management.metrics.export.influx")
public class InfluxProperties extends StepRegistryProperties {

	/**
	 * Tag that will be mapped to "host" when shipping metrics to Influx.
	 */
	private String db = "mydb";

	/**
	 * Write consistency for each point.
	 */
	private InfluxConsistency consistency = InfluxConsistency.ONE;

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
	 * Time period for which Influx should retain data in the current database. For
	 * instance 7d, check the influx documentation for more details on the duration
	 * format.
	 */
	private String retentionDuration;

	/**
	 * How many copies of the data are stored in the cluster. Must be 1 for a single node
	 * instance.
	 */
	private Integer retentionReplicationFactor;

	/**
	 * Time range covered by a shard group. For instance 2w, check the influx
	 * documentation for more details on the duration format.
	 */
	private String retentionShardDuration;

	/**
	 * URI of the Influx server.
	 */
	private String uri = "http://localhost:8086";

	/**
	 * Whether to enable GZIP compression of metrics batches published to Influx.
	 */
	private boolean compressed = true;

	/**
	 * Whether to create the Influx database if it does not exist before attempting to
	 * publish metrics to it.
	 */
	private boolean autoCreateDb = true;

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

	public String getRetentionDuration() {
		return this.retentionDuration;
	}

	public void setRetentionDuration(String retentionDuration) {
		this.retentionDuration = retentionDuration;
	}

	public Integer getRetentionReplicationFactor() {
		return this.retentionReplicationFactor;
	}

	public void setRetentionReplicationFactor(Integer retentionReplicationFactor) {
		this.retentionReplicationFactor = retentionReplicationFactor;
	}

	public String getRetentionShardDuration() {
		return this.retentionShardDuration;
	}

	public void setRetentionShardDuration(String retentionShardDuration) {
		this.retentionShardDuration = retentionShardDuration;
	}

	public String getUri() {
		return this.uri;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}

	public boolean isCompressed() {
		return this.compressed;
	}

	public void setCompressed(boolean compressed) {
		this.compressed = compressed;
	}

	public boolean isAutoCreateDb() {
		return this.autoCreateDb;
	}

	public void setAutoCreateDb(boolean autoCreateDb) {
		this.autoCreateDb = autoCreateDb;
	}

}
