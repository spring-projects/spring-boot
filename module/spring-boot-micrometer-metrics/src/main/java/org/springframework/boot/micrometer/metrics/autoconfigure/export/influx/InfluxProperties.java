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

package org.springframework.boot.micrometer.metrics.autoconfigure.export.influx;

import io.micrometer.influx.InfluxApiVersion;
import io.micrometer.influx.InfluxConsistency;
import org.jspecify.annotations.Nullable;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.micrometer.metrics.autoconfigure.export.properties.StepRegistryProperties;

/**
 * {@link ConfigurationProperties @ConfigurationProperties} for configuring Influx metrics
 * export.
 *
 * @author Jon Schneider
 * @author Stephane Nicoll
 * @since 4.0.0
 */
@ConfigurationProperties("management.influx.metrics.export")
public class InfluxProperties extends StepRegistryProperties {

	/**
	 * Database to send metrics to. InfluxDB v1 only.
	 */
	private String db = "mydb";

	/**
	 * Write consistency for each point.
	 */
	private InfluxConsistency consistency = InfluxConsistency.ONE;

	/**
	 * Login user of the Influx server. InfluxDB v1 only.
	 */
	private @Nullable String userName;

	/**
	 * Login password of the Influx server. InfluxDB v1 only.
	 */
	private @Nullable String password;

	/**
	 * Retention policy to use (Influx writes to the DEFAULT retention policy if one is
	 * not specified). InfluxDB v1 only.
	 */
	private @Nullable String retentionPolicy;

	/**
	 * Time period for which Influx should retain data in the current database. For
	 * instance 7d, check the influx documentation for more details on the duration
	 * format. InfluxDB v1 only.
	 */
	private @Nullable String retentionDuration;

	/**
	 * How many copies of the data are stored in the cluster. Must be 1 for a single node
	 * instance. InfluxDB v1 only.
	 */
	private @Nullable Integer retentionReplicationFactor;

	/**
	 * Time range covered by a shard group. For instance 2w, check the influx
	 * documentation for more details on the duration format. InfluxDB v1 only.
	 */
	private @Nullable String retentionShardDuration;

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
	 * publish metrics to it. InfluxDB v1 only.
	 */
	private boolean autoCreateDb = true;

	/**
	 * API version of InfluxDB to use. Defaults to 'v1' unless an org is configured. If an
	 * org is configured, defaults to 'v2'.
	 */
	private @Nullable InfluxApiVersion apiVersion;

	/**
	 * Org to write metrics to. InfluxDB v2 only.
	 */
	private @Nullable String org;

	/**
	 * Bucket for metrics. Use either the bucket name or ID. Defaults to the value of the
	 * db property if not set. InfluxDB v2 only.
	 */
	private @Nullable String bucket;

	/**
	 * Authentication token to use with calls to the InfluxDB backend. For InfluxDB v1,
	 * the Bearer scheme is used. For v2, the Token scheme is used.
	 */
	private @Nullable String token;

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

	public @Nullable String getUserName() {
		return this.userName;
	}

	public void setUserName(@Nullable String userName) {
		this.userName = userName;
	}

	public @Nullable String getPassword() {
		return this.password;
	}

	public void setPassword(@Nullable String password) {
		this.password = password;
	}

	public @Nullable String getRetentionPolicy() {
		return this.retentionPolicy;
	}

	public void setRetentionPolicy(@Nullable String retentionPolicy) {
		this.retentionPolicy = retentionPolicy;
	}

	public @Nullable String getRetentionDuration() {
		return this.retentionDuration;
	}

	public void setRetentionDuration(@Nullable String retentionDuration) {
		this.retentionDuration = retentionDuration;
	}

	public @Nullable Integer getRetentionReplicationFactor() {
		return this.retentionReplicationFactor;
	}

	public void setRetentionReplicationFactor(@Nullable Integer retentionReplicationFactor) {
		this.retentionReplicationFactor = retentionReplicationFactor;
	}

	public @Nullable String getRetentionShardDuration() {
		return this.retentionShardDuration;
	}

	public void setRetentionShardDuration(@Nullable String retentionShardDuration) {
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

	public @Nullable InfluxApiVersion getApiVersion() {
		return this.apiVersion;
	}

	public void setApiVersion(@Nullable InfluxApiVersion apiVersion) {
		this.apiVersion = apiVersion;
	}

	public @Nullable String getOrg() {
		return this.org;
	}

	public void setOrg(@Nullable String org) {
		this.org = org;
	}

	public @Nullable String getBucket() {
		return this.bucket;
	}

	public void setBucket(@Nullable String bucket) {
		this.bucket = bucket;
	}

	public @Nullable String getToken() {
		return this.token;
	}

	public void setToken(@Nullable String token) {
		this.token = token;
	}

}
