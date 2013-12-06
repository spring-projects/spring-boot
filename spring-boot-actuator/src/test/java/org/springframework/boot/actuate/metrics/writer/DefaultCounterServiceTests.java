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
import org.springframework.boot.actuate.metrics.writer.DefaultCounterService;
import org.springframework.boot.actuate.metrics.writer.Delta;
import org.springframework.boot.actuate.metrics.writer.MetricWriter;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link DefaultCounterService}.
 */
public class DefaultCounterServiceTests {

	private MetricWriter repository = mock(MetricWriter.class);

	private DefaultCounterService service = new DefaultCounterService(this.repository);

	@Test
	public void incrementPrependsCounter() {
		this.service.increment("foo");
		@SuppressWarnings("rawtypes")
		ArgumentCaptor<Delta> captor = ArgumentCaptor.forClass(Delta.class);
		verify(this.repository).increment(captor.capture());
		assertEquals("counter.foo", captor.getValue().getName());
	}

	@Test
	public void decrementPrependsCounter() {
		this.service.decrement("foo");
		@SuppressWarnings("rawtypes")
		ArgumentCaptor<Delta> captor = ArgumentCaptor.forClass(Delta.class);
		verify(this.repository).increment(captor.capture());
		assertEquals("counter.foo", captor.getValue().getName());
		assertEquals(-1L, captor.getValue().getValue());
	}
}
