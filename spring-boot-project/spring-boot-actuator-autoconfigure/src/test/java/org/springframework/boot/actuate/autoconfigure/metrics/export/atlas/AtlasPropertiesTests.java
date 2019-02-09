/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.metrics.export.atlas;

import com.netflix.spectator.atlas.AtlasConfig;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link AtlasProperties}.
 *
 * @author Stephane Nicoll
 */
public class AtlasPropertiesTests {

	@Test
	public void defaultValuesAreConsistent() {
		AtlasProperties properties = new AtlasProperties();
		AtlasConfig config = (key) -> null;
		assertThat(properties.getStep()).isEqualTo(config.step());
		assertThat(properties.isEnabled()).isEqualTo(config.enabled());
		assertThat(properties.getConnectTimeout()).isEqualTo(config.connectTimeout());
		assertThat(properties.getReadTimeout()).isEqualTo(config.readTimeout());
		assertThat(properties.getNumThreads()).isEqualTo(config.numThreads());
		assertThat(properties.getBatchSize()).isEqualTo(config.batchSize());
		assertThat(properties.getUri()).isEqualTo(config.uri());
		assertThat(properties.getMeterTimeToLive()).isEqualTo(config.meterTTL());
		assertThat(properties.isLwcEnabled()).isEqualTo(config.lwcEnabled());
		assertThat(properties.getConfigRefreshFrequency())
				.isEqualTo(config.configRefreshFrequency());
		assertThat(properties.getConfigTimeToLive()).isEqualTo(config.configTTL());
		assertThat(properties.getConfigUri()).isEqualTo(config.configUri());
		assertThat(properties.getEvalUri()).isEqualTo(config.evalUri());
	}

}
