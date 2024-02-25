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
import io.micrometer.influx.InfluxConfig;
import io.micrometer.influx.InfluxConsistency;

import org.springframework.boot.actuate.autoconfigure.metrics.export.properties.StepRegistryPropertiesConfigAdapter;

/**
 * Adapter to convert {@link InfluxProperties} to an {@link InfluxConfig}.
 *
 * @author Jon Schneider
 * @author Phillip Webb
 */
class InfluxPropertiesConfigAdapter extends StepRegistryPropertiesConfigAdapter<InfluxProperties>
		implements InfluxConfig {

	/**
     * Constructs a new InfluxPropertiesConfigAdapter with the specified InfluxProperties.
     *
     * @param properties the InfluxProperties to be used for configuring the adapter
     */
    InfluxPropertiesConfigAdapter(InfluxProperties properties) {
		super(properties);
	}

	/**
     * Returns the prefix for the InfluxDB metrics export configuration properties.
     * The prefix is used to group the properties related to InfluxDB metrics export.
     *
     * @return the prefix for the InfluxDB metrics export configuration properties
     */
    @Override
	public String prefix() {
		return "management.influx.metrics.export";
	}

	/**
     * Returns the name of the database to be used for InfluxDB.
     * 
     * @return the name of the database
     */
    @Override
	public String db() {
		return get(InfluxProperties::getDb, InfluxConfig.super::db);
	}

	/**
     * Returns the consistency level for the InfluxDB configuration.
     * 
     * @return the consistency level
     */
    @Override
	public InfluxConsistency consistency() {
		return get(InfluxProperties::getConsistency, InfluxConfig.super::consistency);
	}

	/**
     * Returns the username for connecting to the Influx database.
     * 
     * @return the username for connecting to the Influx database
     */
    @Override
	public String userName() {
		return get(InfluxProperties::getUserName, InfluxConfig.super::userName);
	}

	/**
     * Returns the password for the InfluxDB connection.
     * 
     * @return the password for the InfluxDB connection
     */
    @Override
	public String password() {
		return get(InfluxProperties::getPassword, InfluxConfig.super::password);
	}

	/**
     * Returns the retention policy for the InfluxDB configuration.
     * 
     * @return the retention policy
     */
    @Override
	public String retentionPolicy() {
		return get(InfluxProperties::getRetentionPolicy, InfluxConfig.super::retentionPolicy);
	}

	/**
     * Returns the retention replication factor.
     * 
     * @return the retention replication factor
     */
    @Override
	public Integer retentionReplicationFactor() {
		return get(InfluxProperties::getRetentionReplicationFactor, InfluxConfig.super::retentionReplicationFactor);
	}

	/**
     * Returns the retention duration for the InfluxDB configuration.
     * 
     * @return the retention duration
     */
    @Override
	public String retentionDuration() {
		return get(InfluxProperties::getRetentionDuration, InfluxConfig.super::retentionDuration);
	}

	/**
     * Returns the retention shard duration value from the InfluxProperties configuration.
     * If the value is not present in the InfluxProperties configuration, it falls back to the default value
     * provided by the InfluxConfig interface.
     *
     * @return the retention shard duration value
     */
    @Override
	public String retentionShardDuration() {
		return get(InfluxProperties::getRetentionShardDuration, InfluxConfig.super::retentionShardDuration);
	}

	/**
     * Returns the URI for connecting to the Influx database.
     * 
     * @return the URI for connecting to the Influx database
     */
    @Override
	public String uri() {
		return get(InfluxProperties::getUri, InfluxConfig.super::uri);
	}

	/**
     * Returns a boolean value indicating whether the data is compressed.
     * 
     * @return true if the data is compressed, false otherwise
     */
    @Override
	public boolean compressed() {
		return get(InfluxProperties::isCompressed, InfluxConfig.super::compressed);
	}

	/**
     * Returns the value of the autoCreateDb property.
     * 
     * @return true if autoCreateDb is enabled, false otherwise
     */
    @Override
	public boolean autoCreateDb() {
		return get(InfluxProperties::isAutoCreateDb, InfluxConfig.super::autoCreateDb);
	}

	/**
     * Returns the API version of the InfluxDB.
     * 
     * @return the API version of the InfluxDB
     */
    @Override
	public InfluxApiVersion apiVersion() {
		return get(InfluxProperties::getApiVersion, InfluxConfig.super::apiVersion);
	}

	/**
     * Returns the organization name.
     * 
     * @return the organization name
     */
    @Override
	public String org() {
		return get(InfluxProperties::getOrg, InfluxConfig.super::org);
	}

	/**
     * Returns the bucket value from the InfluxProperties or the default value from the InfluxConfig.
     * 
     * @return the bucket value
     */
    @Override
	public String bucket() {
		return get(InfluxProperties::getBucket, InfluxConfig.super::bucket);
	}

	/**
     * Returns the token for accessing the Influx database.
     * 
     * @return the token for accessing the Influx database
     */
    @Override
	public String token() {
		return get(InfluxProperties::getToken, InfluxConfig.super::token);
	}

}
