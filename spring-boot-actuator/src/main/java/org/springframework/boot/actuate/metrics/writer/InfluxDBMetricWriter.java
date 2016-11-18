/*
 * Copyright 2012-2013 the original author or authors.
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

package org.springframework.boot.actuate.metrics.writer;

import java.util.concurrent.TimeUnit;

import org.influxdb.InfluxDB;
import org.influxdb.dto.Point;

import org.springframework.boot.actuate.metrics.Metric;
import org.springframework.util.Assert;

/**
 * A {@link GaugeWriter} that writes the metric updates to InfluxDB.
 *
 * @author Mateusz Klimaszewski
 */
public class InfluxDBMetricWriter implements GaugeWriter {

	private static final String DEFAULT_DATABASE_NAME = "metrics";
	private static final int DEFAULT_BATCH_ACTIONS = 500;
	private static final int DEFAULT_FLUSH_DURATION = 30;

	private final InfluxDB influxDB;
	private final String databaseName;

	private InfluxDBMetricWriter(Builder builder) {
		this.influxDB = builder.influxDB;
		this.databaseName = builder.databaseName;
		this.influxDB.createDatabase(this.databaseName);
		this.influxDB.enableBatch(builder.batchActions, builder.flushDuration,
				builder.flushDurationTimeUnit);
		this.influxDB.setLogLevel(builder.logLevel);
	}

	@Override
	public void set(Metric<?> value) {
		Point point = Point.measurement(value.getName())
				.time(value.getTimestamp().getTime(), TimeUnit.MILLISECONDS)
				.addField("value", value.getValue())
				.build();
		this.influxDB.write(this.databaseName, value.getName(), point);
	}

	/**
	 * {@link InfluxDBMetricWriter} builder with possibility to change default arguments
	 */
	public static class Builder {
		private final InfluxDB influxDB;
		private String databaseName = DEFAULT_DATABASE_NAME;
		private int batchActions = DEFAULT_BATCH_ACTIONS;
		private int flushDuration = DEFAULT_FLUSH_DURATION;
		private TimeUnit flushDurationTimeUnit = TimeUnit.SECONDS;
		private InfluxDB.LogLevel logLevel = InfluxDB.LogLevel.BASIC;

		public Builder(InfluxDB influxDB) {
			Assert.notNull(influxDB, "InfluxDB must not be null");
			this.influxDB = influxDB;
		}

		public Builder databaseName(String databaseName) {
			this.databaseName = databaseName;
			return this;
		}

		public Builder batchActions(int batchActions) {
			this.batchActions = batchActions;
			return this;
		}

		public Builder flushDuration(int flushDuration, TimeUnit flushDurationTimeUnit) {
			this.flushDuration = flushDuration;
			this.flushDurationTimeUnit = flushDurationTimeUnit;
			return this;
		}

		public Builder logLevel(InfluxDB.LogLevel logLevel) {
			this.logLevel = logLevel;
			return this;
		}

		public InfluxDBMetricWriter build() {
			return new InfluxDBMetricWriter(this);
		}
	}
}
