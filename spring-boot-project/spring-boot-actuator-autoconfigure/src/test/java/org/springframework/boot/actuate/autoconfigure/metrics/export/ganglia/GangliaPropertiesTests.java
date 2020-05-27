/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.metrics.export.ganglia;

import io.micrometer.ganglia.GangliaConfig;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link GangliaProperties}.
 *
 * @author Stephane Nicoll
 */
class GangliaPropertiesTests {

	@Test
	@SuppressWarnings("deprecation")
	void defaultValuesAreConsistent() {
		GangliaProperties properties = new GangliaProperties();
		GangliaConfig config = GangliaConfig.DEFAULT;
		assertThat(properties.isEnabled()).isEqualTo(config.enabled());
		assertThat(properties.getStep()).isEqualTo(config.step());
		assertThat(properties.getRateUnits()).isEqualTo(config.rateUnits());
		assertThat(properties.getDurationUnits()).isEqualTo(config.durationUnits());
		assertThat(properties.getProtocolVersion()).isEqualTo(config.protocolVersion());
		assertThat(properties.getAddressingMode()).isEqualTo(config.addressingMode());
		assertThat(properties.getTimeToLive()).isEqualTo(config.ttl());
		assertThat(properties.getHost()).isEqualTo(config.host());
		assertThat(properties.getPort()).isEqualTo(config.port());
	}

}
