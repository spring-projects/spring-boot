/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.actuate.metrics.writer;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link DefaultCounterService}.
 *
 * @author Dave Syer
 */
public class DefaultCounterServiceTests {

	private final MetricWriter repository = mock(MetricWriter.class);

	private final DefaultCounterService service = new DefaultCounterService(
			this.repository);

	@Captor
	private ArgumentCaptor<Delta<Number>> captor;

	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
	}

	@Test
	public void incrementWithExistingCounter() {
		this.service.increment("counter.foo");
		verify(this.repository).increment(this.captor.capture());
		assertThat(this.captor.getValue().getName()).isEqualTo("counter.foo");
		assertThat(this.captor.getValue().getValue()).isEqualTo(1L);
	}

	@Test
	public void incrementWithExistingNearCounter() {
		this.service.increment("counter-foo");
		verify(this.repository).increment(this.captor.capture());
		assertThat(this.captor.getValue().getName()).isEqualTo("counter.counter-foo");
		assertThat(this.captor.getValue().getValue()).isEqualTo(1L);
	}

	@Test
	public void incrementPrependsCounter() {
		this.service.increment("foo");
		verify(this.repository).increment(this.captor.capture());
		assertThat(this.captor.getValue().getName()).isEqualTo("counter.foo");
		assertThat(this.captor.getValue().getValue()).isEqualTo(1L);
	}

	@Test
	public void decrementPrependsCounter() {
		this.service.decrement("foo");
		verify(this.repository).increment(this.captor.capture());
		assertThat(this.captor.getValue().getName()).isEqualTo("counter.foo");
		assertThat(this.captor.getValue().getValue()).isEqualTo(-1L);
	}

	@Test
	public void resetResetsCounter() throws Exception {
		this.service.reset("foo");
		verify(this.repository).reset("counter.foo");
	}

}
