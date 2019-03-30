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

package org.springframework.boot.actuate.trace.http;

import java.util.List;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link InMemoryHttpTraceRepository}.
 *
 * @author Dave Syer
 * @author Andy Wilkinson
 */
public class InMemoryHttpTraceRepositoryTests {

	private final InMemoryHttpTraceRepository repository = new InMemoryHttpTraceRepository();

	@Test
	public void capacityLimited() {
		this.repository.setCapacity(2);
		this.repository.add(new HttpTrace(createRequest("GET")));
		this.repository.add(new HttpTrace(createRequest("POST")));
		this.repository.add(new HttpTrace(createRequest("DELETE")));
		List<HttpTrace> traces = this.repository.findAll();
		assertThat(traces).hasSize(2);
		assertThat(traces.get(0).getRequest().getMethod()).isEqualTo("DELETE");
		assertThat(traces.get(1).getRequest().getMethod()).isEqualTo("POST");
	}

	@Test
	public void reverseFalse() {
		this.repository.setReverse(false);
		this.repository.setCapacity(2);
		this.repository.add(new HttpTrace(createRequest("GET")));
		this.repository.add(new HttpTrace(createRequest("POST")));
		this.repository.add(new HttpTrace(createRequest("DELETE")));
		List<HttpTrace> traces = this.repository.findAll();
		assertThat(traces).hasSize(2);
		assertThat(traces.get(0).getRequest().getMethod()).isEqualTo("POST");
		assertThat(traces.get(1).getRequest().getMethod()).isEqualTo("DELETE");
	}

	private TraceableRequest createRequest(String method) {
		TraceableRequest request = mock(TraceableRequest.class);
		given(request.getMethod()).willReturn(method);
		return request;
	}

}
