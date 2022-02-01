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

package org.springframework.boot.actuate.influx;

import java.io.IOException;

import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBException;
import org.influxdb.dto.Pong;
import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link InfluxDbHealthIndicator}.
 *
 * @author Eddú Meléndez
 */
class InfluxDbHealthIndicatorTests {

	@Test
	void influxDbIsUp() {
		Pong pong = mock(Pong.class);
		given(pong.getVersion()).willReturn("0.9");
		InfluxDB influxDb = mock(InfluxDB.class);
		given(influxDb.ping()).willReturn(pong);
		InfluxDbHealthIndicator healthIndicator = new InfluxDbHealthIndicator(influxDb);
		Health health = healthIndicator.health();
		assertThat(health.getStatus()).isEqualTo(Status.UP);
		assertThat(health.getDetails().get("version")).isEqualTo("0.9");
		then(influxDb).should().ping();
	}

	@Test
	void influxDbIsDown() {
		InfluxDB influxDb = mock(InfluxDB.class);
		given(influxDb.ping()).willThrow(new InfluxDBException(new IOException("Connection failed")));
		InfluxDbHealthIndicator healthIndicator = new InfluxDbHealthIndicator(influxDb);
		Health health = healthIndicator.health();
		assertThat(health.getStatus()).isEqualTo(Status.DOWN);
		assertThat((String) health.getDetails().get("error")).contains("Connection failed");
		then(influxDb).should().ping();
	}

}
