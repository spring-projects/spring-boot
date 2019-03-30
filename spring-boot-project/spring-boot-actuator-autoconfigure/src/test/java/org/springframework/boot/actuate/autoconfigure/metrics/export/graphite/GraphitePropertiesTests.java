/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.metrics.export.graphite;

import io.micrometer.graphite.GraphiteConfig;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link GraphiteProperties}.
 *
 * @author Stephane Nicoll
 */
public class GraphitePropertiesTests {

	@Test
	public void defaultValuesAreConsistent() {
		GraphiteProperties properties = new GraphiteProperties();
		GraphiteConfig config = GraphiteConfig.DEFAULT;
		assertThat(properties.isEnabled()).isEqualTo(config.enabled());
		assertThat(properties.getStep()).isEqualTo(config.step());
		assertThat(properties.getRateUnits()).isEqualTo(config.rateUnits());
		assertThat(properties.getDurationUnits()).isEqualTo(config.durationUnits());
		assertThat(properties.getHost()).isEqualTo(config.host());
		assertThat(properties.getPort()).isEqualTo(config.port());
		assertThat(properties.getProtocol()).isEqualTo(config.protocol());
		assertThat(properties.getTagsAsPrefix()).isEqualTo(config.tagsAsPrefix());
	}

}
