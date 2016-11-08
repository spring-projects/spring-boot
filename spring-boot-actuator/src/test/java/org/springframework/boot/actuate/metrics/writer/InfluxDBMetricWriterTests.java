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
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import org.springframework.boot.actuate.metrics.Metric;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link InfluxDBMetricWriter}.
 *
 * @author Mateusz Klimaszewski
 */
public class InfluxDBMetricWriterTests {

	private InfluxDB influxDB = mock(InfluxDB.class);
	private InfluxDBMetricWriter influxDBMetricWriter;

	@Test
	public void builderNonDefaultOptions() {
		this.influxDBMetricWriter = new InfluxDBMetricWriter.Builder(this.influxDB)
				.databaseName("testDatabaseName")
				.batchActions(2000)
				.flushDuration(10, TimeUnit.MILLISECONDS)
				.logLevel(InfluxDB.LogLevel.FULL)
				.build();
		ArgumentCaptor<String> databaseNameCaptor = ArgumentCaptor.forClass(
				String.class);
		ArgumentCaptor<Integer> bashActionsCaptor = ArgumentCaptor.forClass(
				Integer.class);
		ArgumentCaptor<Integer> flushDurationCaptor = ArgumentCaptor.forClass(
				Integer.class);
		ArgumentCaptor<TimeUnit> flushDurationTimeUnitCaptor = ArgumentCaptor.forClass(
				TimeUnit.class);
		ArgumentCaptor<InfluxDB.LogLevel> logLevelCaptor = ArgumentCaptor.forClass(
				InfluxDB.LogLevel.class);
		verify(this.influxDB).createDatabase(databaseNameCaptor.capture());
		verify(this.influxDB).enableBatch(bashActionsCaptor.capture(),
				flushDurationCaptor.capture(), flushDurationTimeUnitCaptor.capture());
		verify(this.influxDB).setLogLevel(logLevelCaptor.capture());
		assertThat("testDatabaseName").isEqualTo(databaseNameCaptor.getValue());
		assertThat(2000).isEqualTo(bashActionsCaptor.getValue().intValue());
		assertThat(10).isEqualTo(flushDurationCaptor.getValue().intValue());
		assertThat(TimeUnit.MILLISECONDS).isEqualTo(flushDurationTimeUnitCaptor
				.getValue());
		assertThat(InfluxDB.LogLevel.FULL).isEqualTo(logLevelCaptor.getValue());
	}

	@Test
	public void setMetric() {
		this.influxDBMetricWriter = new InfluxDBMetricWriter.Builder(this.influxDB)
				.build();
		Metric<Number> metric = new Metric<Number>("testName", 1);
		this.influxDBMetricWriter.set(metric);
		verify(this.influxDB, times(1)).write(anyString(), eq(metric.getName()),
				any(Point.class));
	}
}
