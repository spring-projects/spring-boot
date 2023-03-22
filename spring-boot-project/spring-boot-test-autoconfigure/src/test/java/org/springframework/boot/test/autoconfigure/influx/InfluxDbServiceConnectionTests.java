/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.test.autoconfigure.influx;

import org.influxdb.InfluxDB;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.InfluxDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.influx.InfluxDbAutoConfiguration;
import org.springframework.boot.test.autoconfigure.influx.InfluxDbServiceConnectionTests.TestConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testsupport.testcontainers.DockerImageNames;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link InfluxDbServiceConnection}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
@SpringBootTest(classes = TestConfiguration.class)
@Testcontainers(disabledWithoutDocker = true)
class InfluxDbServiceConnectionTests {

	@Container
	@InfluxDbServiceConnection
	static final InfluxDBContainer<?> influxDbService = new InfluxDBContainer<>(DockerImageNames.influxDb());

	@Autowired
	InfluxDB influxDb;

	@Test
	void connectionCanBeMadeToInfluxDbContainer() {
		assertThat(this.influxDb.version()).isEqualTo("v" + DockerImageNames.influxDb().getVersionPart());
	}

	@ImportAutoConfiguration(InfluxDbAutoConfiguration.class)
	static class TestConfiguration {

	}

}
