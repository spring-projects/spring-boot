/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.metrics.export.simple;

import io.micrometer.core.instrument.simple.SimpleConfig;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Stephane Nicoll
 */
public class SimplePropertiesTests {

	@Test
	public void defaultValuesAreConsistent() {
		SimpleProperties properties = new SimpleProperties();
		SimpleConfig config = SimpleConfig.DEFAULT;
		assertThat(properties.getStep()).isEqualTo(config.step());
		assertThat(properties.getMode()).isEqualTo(config.mode());
	}

}
