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

import org.influxdb.BatchOptions;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link InfluxDbProperties}.
 *
 * @author Eddú Meléndez
 */
class InfluxDbPropertiesTests {

	@Test
	void defaultValuesAreConsistent() {
		InfluxDbProperties properties = new InfluxDbProperties();
		BatchOptions batchOptions = BatchOptions.DEFAULTS;
		assertThat(properties.getBatch().getActions()).isEqualTo(batchOptions.getActions());
		assertThat(Long.valueOf(properties.getBatch().getFlushDuration().toMillis()).intValue())
				.isEqualTo(batchOptions.getFlushDuration());
		assertThat(Long.valueOf(properties.getBatch().getJitterDuration().toMillis()).intValue())
				.isEqualTo(batchOptions.getJitterDuration());
		assertThat(properties.getBatch().getBufferLimit()).isEqualTo(batchOptions.getBufferLimit());
		assertThat(properties.getBatch().getConsistency()).isEqualTo(batchOptions.getConsistency());
		assertThat(properties.getBatch().getPrecision()).isEqualTo(batchOptions.getPrecision());
		assertThat(properties.getBatch().isDropActionsOnQueueExhaustion())
				.isEqualTo(batchOptions.isDropActionsOnQueueExhaustion());
	}

}
