/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.actuate.trace;

import java.util.Collections;
import java.util.List;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link InMemoryTraceRepository}.
 *
 * @author Dave Syer
 */
public class InMemoryTraceRepositoryTests {

	private final InMemoryTraceRepository repository = new InMemoryTraceRepository();

	@Test
	public void capacityLimited() {
		this.repository.setCapacity(2);
		this.repository.add(Collections.singletonMap("foo", "bar"));
		this.repository.add(Collections.singletonMap("bar", "foo"));
		this.repository.add(Collections.singletonMap("bar", "bar"));
		List<Trace> traces = this.repository.findAll();
		assertThat(traces).hasSize(2);
		assertThat(traces.get(0).getInfo().get("bar")).isEqualTo("bar");
		assertThat(traces.get(1).getInfo().get("bar")).isEqualTo("foo");
	}

	@Test
	public void reverseFalse() {
		this.repository.setReverse(false);
		this.repository.setCapacity(2);
		this.repository.add(Collections.singletonMap("foo", "bar"));
		this.repository.add(Collections.singletonMap("bar", "foo"));
		this.repository.add(Collections.singletonMap("bar", "bar"));
		List<Trace> traces = this.repository.findAll();
		assertThat(traces).hasSize(2);
		assertThat(traces.get(1).getInfo().get("bar")).isEqualTo("bar");
		assertThat(traces.get(0).getInfo().get("bar")).isEqualTo("foo");
	}

}
