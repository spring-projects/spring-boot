/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.metrics.export.statsd;

import io.micrometer.statsd.StatsdConfig;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link StatsdProperties}.
 *
 * @author Stephane Nicoll
 */
public class StatsdPropertiesTests {

	@Test
	public void defaultValuesAreConsistent() {
		StatsdProperties properties = new StatsdProperties();
		StatsdConfig config = StatsdConfig.DEFAULT;
		assertThat(properties.isEnabled()).isEqualTo(config.enabled());
		assertThat(properties.getFlavor()).isEqualTo(config.flavor());
		assertThat(properties.getHost()).isEqualTo(config.host());
		assertThat(properties.getPort()).isEqualTo(config.port());
		assertThat(properties.getMaxPacketLength()).isEqualTo(config.maxPacketLength());
		assertThat(properties.getPollingFrequency()).isEqualTo(config.pollingFrequency());
		assertThat(properties.isPublishUnchangedMeters()).isEqualTo(config.publishUnchangedMeters());
	}

}
