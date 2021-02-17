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

package org.springframework.boot.autoconfigure.influx;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.influxdb.InfluxDB;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for InfluxDB.
 *
 * @author Sergey Kuptsov
 * @author Stephane Nicoll
 * @author Eddú Meléndez
 * @since 2.0.0
 */
@ConfigurationProperties(prefix = "spring.influx")
public class InfluxDbProperties {

	/**
	 * URL of the InfluxDB instance to which to connect.
	 */
	private String url;

	/**
	 * Login user.
	 */
	private String user;

	/**
	 * Login password.
	 */
	private String password;

	/**
	 * Consistency level.
	 */
	private InfluxDB.ConsistencyLevel consistency;

	/**
	 * Database name.
	 */
	private String database;

	/**
	 * Log level.
	 */
	private InfluxDB.LogLevel log;

	/**
	 * Retention policy.
	 */
	private String retentionPolicy;

	/**
	 * Whether to enable Gzip compression.
	 */
	private boolean gzipEnabled;

	/**
	 * Batch configuration.
	 */
	private final Batch batch = new Batch();

	public String getUrl() {
		return this.url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getUser() {
		return this.user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getPassword() {
		return this.password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public InfluxDB.ConsistencyLevel getConsistency() {
		return this.consistency;
	}

	public void setConsistency(InfluxDB.ConsistencyLevel consistency) {
		this.consistency = consistency;
	}

	public String getDatabase() {
		return this.database;
	}

	public void setDatabase(String database) {
		this.database = database;
	}

	public InfluxDB.LogLevel getLog() {
		return this.log;
	}

	public void setLog(InfluxDB.LogLevel log) {
		this.log = log;
	}

	public String getRetentionPolicy() {
		return this.retentionPolicy;
	}

	public void setRetentionPolicy(String retentionPolicy) {
		this.retentionPolicy = retentionPolicy;
	}

	public boolean isGzipEnabled() {
		return this.gzipEnabled;
	}

	public void setGzipEnabled(boolean gzipEnabled) {
		this.gzipEnabled = gzipEnabled;
	}

	public Batch getBatch() {
		return this.batch;
	}

	public static class Batch {

		/**
		 * Whether to enable Batch configuration.
		 */
		private boolean enabled;

		/**
		 * Number of actions to collect.
		 */
		private int actions = 1000;

		/**
		 * Time to wait.
		 */
		private Duration flushDuration = Duration.ofMillis(1000);

		/**
		 * Time to jitter the batch flush interval.
		 */
		private Duration jitterDuration = Duration.ofMillis(0);

		/**
		 * Number of points stored in the retry buffer.
		 */
		private int bufferLimit = 10000;

		/**
		 * Cluster consistency.
		 */
		private InfluxDB.ConsistencyLevel consistency = InfluxDB.ConsistencyLevel.ONE;

		/**
		 * Precision to use for the whole batch.
		 */
		private TimeUnit precision = TimeUnit.NANOSECONDS;

		/**
		 * Whether to enable dropped actions.
		 */
		private boolean dropActionsOnQueueExhaustion = false;

		public boolean isEnabled() {
			return this.enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		public int getActions() {
			return this.actions;
		}

		public void setActions(int actions) {
			this.actions = actions;
		}

		public Duration getFlushDuration() {
			return this.flushDuration;
		}

		public void setFlushDuration(Duration flushDuration) {
			this.flushDuration = flushDuration;
		}

		public Duration getJitterDuration() {
			return this.jitterDuration;
		}

		public void setJitterDuration(Duration jitterDuration) {
			this.jitterDuration = jitterDuration;
		}

		public int getBufferLimit() {
			return this.bufferLimit;
		}

		public void setBufferLimit(int bufferLimit) {
			this.bufferLimit = bufferLimit;
		}

		public InfluxDB.ConsistencyLevel getConsistency() {
			return this.consistency;
		}

		public void setConsistency(InfluxDB.ConsistencyLevel consistency) {
			this.consistency = consistency;
		}

		public TimeUnit getPrecision() {
			return this.precision;
		}

		public void setPrecision(TimeUnit precision) {
			this.precision = precision;
		}

		public boolean isDropActionsOnQueueExhaustion() {
			return this.dropActionsOnQueueExhaustion;
		}

		public void setDropActionsOnQueueExhaustion(boolean dropActionsOnQueueExhaustion) {
			this.dropActionsOnQueueExhaustion = dropActionsOnQueueExhaustion;
		}

	}

}
