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

package org.springframework.boot.actuate.metrics.web.tomcat;

import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;

/**
 * Tests for {@link TomcatMetricsBinder}.
 *
 * @author Andy Wilkinson
 */
class TomcatMetricsBinderTests {

	private final MeterRegistry meterRegistry = mock(MeterRegistry.class);

	@Test
	void destroySucceedsWhenCalledBeforeApplicationHasStarted() {
		new TomcatMetricsBinder(this.meterRegistry).destroy();
	}

}
