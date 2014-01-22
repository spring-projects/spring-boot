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

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.actuate.metrics.Metric;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link DefaultGaugeService}.
 */
public class DefaultGaugeServiceTests {

	private final MetricWriter repository = mock(MetricWriter.class);

	private final DefaultGaugeService service = new DefaultGaugeService(this.repository);

	@Test
	public void setPrependsGauge() {
		this.service.submit("foo", 2.3);
		@SuppressWarnings("rawtypes")
		ArgumentCaptor<Metric> captor = ArgumentCaptor.forClass(Metric.class);
		verify(this.repository).set(captor.capture());
		assertEquals("gauge.foo", captor.getValue().getName());
		assertEquals(2.3, captor.getValue().getValue());
	}

}
