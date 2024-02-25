/*
 * Copyright 2012-2022 the original author or authors.
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

import io.micrometer.influx.InfluxApiVersion;
import io.micrometer.influx.InfluxConsistency;

import org.springframework.boot.actuate.autoconfigure.metrics.export.properties.StepRegistryProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@link ConfigurationProperties @ConfigurationProperties} for configuring Influx metrics
 * export.
 *
 * @author Jon Schneider
 * @author Stephane Nicoll
 * @since 2.0.0
 */
@ConfigurationProperties(prefix = "management.influx.metrics.export")
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
	private String userName;

	/**
	 * Login password of the Influx server. InfluxDB v1 only.
	 */
	private String password;

	/**
	 * Retention policy to use (Influx writes to the DEFAULT retention policy if one is
	 * not specified). InfluxDB v1 only.
	 */
	private String retentionPolicy;

	/**
	 * Time period for which Influx should retain data in the current database. For
	 * instance 7d, check the influx documentation for more details on the duration
	 * format. InfluxDB v1 only.
	 */
	private String retentionDuration;

	/**
	 * How many copies of the data are stored in the cluster. Must be 1 for a single node
	 * instance. InfluxDB v1 only.
	 */
	private Integer retentionReplicationFactor;

	/**
	 * Time range covered by a shard group. For instance 2w, check the influx
	 * documentation for more details on the duration format. InfluxDB v1 only.
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
	 * publish metrics to it. InfluxDB v1 only.
	 */
	private boolean autoCreateDb = true;

	/**
	 * API version of InfluxDB to use. Defaults to 'v1' unless an org is configured. If an
	 * org is configured, defaults to 'v2'.
	 */
	private InfluxApiVersion apiVersion;

	/**
	 * Org to write metrics to. InfluxDB v2 only.
	 */
	private String org;

	/**
	 * Bucket for metrics. Use either the bucket name or ID. Defaults to the value of the
	 * db property if not set. InfluxDB v2 only.
	 */
	private String bucket;

	/**
	 * Authentication token to use with calls to the InfluxDB backend. For InfluxDB v1,
	 * the Bearer scheme is used. For v2, the Token scheme is used.
	 */
	private String token;

	/**
     * Returns the name of the database.
     *
     * @return the name of the database
     */
    public String getDb() {
		return this.db;
	}

	/**
     * Sets the name of the database to be used.
     * 
     * @param db the name of the database
     */
    public void setDb(String db) {
		this.db = db;
	}

	/**
     * Returns the consistency level for InfluxDB.
     *
     * @return the consistency level
     */
    public InfluxConsistency getConsistency() {
		return this.consistency;
	}

	/**
     * Sets the consistency level for InfluxDB queries.
     * 
     * @param consistency the consistency level to be set
     */
    public void setConsistency(InfluxConsistency consistency) {
		this.consistency = consistency;
	}

	/**
     * Returns the username associated with the InfluxProperties object.
     *
     * @return the username
     */
    public String getUserName() {
		return this.userName;
	}

	/**
     * Sets the username for the InfluxProperties.
     * 
     * @param userName the username to be set
     */
    public void setUserName(String userName) {
		this.userName = userName;
	}

	/**
     * Returns the password associated with the InfluxProperties object.
     *
     * @return the password
     */
    public String getPassword() {
		return this.password;
	}

	/**
     * Sets the password for the InfluxProperties object.
     * 
     * @param password the password to be set
     */
    public void setPassword(String password) {
		this.password = password;
	}

	/**
     * Returns the retention policy of the InfluxProperties.
     *
     * @return the retention policy
     */
    public String getRetentionPolicy() {
		return this.retentionPolicy;
	}

	/**
     * Sets the retention policy for the InfluxProperties.
     * 
     * @param retentionPolicy the retention policy to be set
     */
    public void setRetentionPolicy(String retentionPolicy) {
		this.retentionPolicy = retentionPolicy;
	}

	/**
     * Returns the retention duration of the InfluxProperties.
     * 
     * @return the retention duration as a String
     */
    public String getRetentionDuration() {
		return this.retentionDuration;
	}

	/**
     * Sets the retention duration for the InfluxProperties.
     * 
     * @param retentionDuration the retention duration to be set
     */
    public void setRetentionDuration(String retentionDuration) {
		this.retentionDuration = retentionDuration;
	}

	/**
     * Returns the retention replication factor.
     *
     * @return the retention replication factor
     */
    public Integer getRetentionReplicationFactor() {
		return this.retentionReplicationFactor;
	}

	/**
     * Sets the retention replication factor for the InfluxProperties.
     * 
     * @param retentionReplicationFactor the retention replication factor to be set
     */
    public void setRetentionReplicationFactor(Integer retentionReplicationFactor) {
		this.retentionReplicationFactor = retentionReplicationFactor;
	}

	/**
     * Returns the retention shard duration.
     * 
     * @return the retention shard duration
     */
    public String getRetentionShardDuration() {
		return this.retentionShardDuration;
	}

	/**
     * Sets the retention shard duration for the InfluxProperties.
     * 
     * @param retentionShardDuration the retention shard duration to be set
     */
    public void setRetentionShardDuration(String retentionShardDuration) {
		this.retentionShardDuration = retentionShardDuration;
	}

	/**
     * Returns the URI of the InfluxProperties.
     *
     * @return the URI of the InfluxProperties
     */
    public String getUri() {
		return this.uri;
	}

	/**
     * Sets the URI for the InfluxProperties.
     * 
     * @param uri the URI to be set
     */
    public void setUri(String uri) {
		this.uri = uri;
	}

	/**
     * Returns a boolean value indicating whether the data is compressed.
     * 
     * @return true if the data is compressed, false otherwise
     */
    public boolean isCompressed() {
		return this.compressed;
	}

	/**
     * Sets the flag indicating whether the data should be compressed.
     * 
     * @param compressed true if the data should be compressed, false otherwise
     */
    public void setCompressed(boolean compressed) {
		this.compressed = compressed;
	}

	/**
     * Returns a boolean value indicating whether the database should be automatically created.
     *
     * @return true if the database should be automatically created, false otherwise
     */
    public boolean isAutoCreateDb() {
		return this.autoCreateDb;
	}

	/**
     * Sets the flag to automatically create the database if it does not exist.
     * 
     * @param autoCreateDb the flag indicating whether to automatically create the database
     */
    public void setAutoCreateDb(boolean autoCreateDb) {
		this.autoCreateDb = autoCreateDb;
	}

	/**
     * Returns the API version of the InfluxDB.
     *
     * @return the API version of the InfluxDB
     */
    public InfluxApiVersion getApiVersion() {
		return this.apiVersion;
	}

	/**
     * Sets the API version for the InfluxProperties.
     * 
     * @param apiVersion the API version to be set
     */
    public void setApiVersion(InfluxApiVersion apiVersion) {
		this.apiVersion = apiVersion;
	}

	/**
     * Returns the organization associated with the InfluxProperties.
     *
     * @return the organization associated with the InfluxProperties
     */
    public String getOrg() {
		return this.org;
	}

	/**
     * Sets the organization for the InfluxProperties.
     * 
     * @param org the organization to set
     */
    public void setOrg(String org) {
		this.org = org;
	}

	/**
     * Returns the name of the bucket.
     *
     * @return the name of the bucket
     */
    public String getBucket() {
		return this.bucket;
	}

	/**
     * Sets the bucket for InfluxProperties.
     * 
     * @param bucket the name of the bucket to be set
     */
    public void setBucket(String bucket) {
		this.bucket = bucket;
	}

	/**
     * Returns the token associated with the InfluxProperties object.
     *
     * @return the token associated with the InfluxProperties object
     */
    public String getToken() {
		return this.token;
	}

	/**
     * Sets the token for authentication.
     * 
     * @param token the token to be set
     */
    public void setToken(String token) {
		this.token = token;
	}

}
