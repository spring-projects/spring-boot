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

import java.time.Duration;

import io.micrometer.influx.InfluxConsistency;

import org.springframework.boot.actuate.autoconfigure.metrics.export.StepRegistryProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@link ConfigurationProperties} for configuring Influx metrics export.
 *
 * @author Jon Schneider
 * @since 2.0.0
 */
@ConfigurationProperties(prefix = "spring.metrics.influx")
public class InfluxProperties extends StepRegistryProperties {
	/**
	 * The tag that will be mapped to "host" when shipping metrics to Influx, or
	 * {@code null} if host should be omitted on publishing.
	 */
	private String db;

	/**
	 * The write consistency for each point.
	 */
	private InfluxConsistency consistency;

	/**
	 * Authenticate requests with this user. If not specified, the registry will not
	 * attempt to present credentials to Influx.
	 */
	private String userName;

	/**
	 * Authenticate requests with this password.
	 */
	private String password;

	/**
	 * Influx writes to the DEFAULT retention policy if one is not specified.
	 */
	private String retentionPolicy;

	/**
	 * The URI for the Influx backend.
	 */
	private String uri;

	/**
	 * Enable GZIP compression of metrics batches published to Influx.
	 */
	private Boolean compressed;

	/**
	 * The bucket filter clamping the bucket domain of timer percentiles histograms to
	 * some max value. This is used to limit the number of buckets shipped to Prometheus
	 * to save on storage.
	 */
	private Duration timerPercentilesMax = Duration.ofMinutes(2);

	/**
	 * The bucket filter clamping the bucket domain of timer percentiles histograms to
	 * some min value. This is used to limit the number of buckets shipped to Prometheus
	 * to save on storage.
	 */
	private Duration timerPercentilesMin = Duration.ofMillis(10);

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

	public Duration getTimerPercentilesMax() {
		return this.timerPercentilesMax;
	}

	public void setTimerPercentilesMax(Duration timerPercentilesMax) {
		this.timerPercentilesMax = timerPercentilesMax;
	}

	public Duration getTimerPercentilesMin() {
		return this.timerPercentilesMin;
	}

	public void setTimerPercentilesMin(Duration timerPercentilesMin) {
		this.timerPercentilesMin = timerPercentilesMin;
	}
}
