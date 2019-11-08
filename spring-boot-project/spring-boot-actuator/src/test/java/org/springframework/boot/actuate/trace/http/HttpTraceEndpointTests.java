/*
 * Copyright 2012-2019 the original author or authors.
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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link HttpTraceEndpoint}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
class HttpTraceEndpointTests {

	@Test
	void trace() {
		HttpTraceRepository repository = new InMemoryHttpTraceRepository();
		repository.add(new HttpTrace(createRequest("GET")));
		List<HttpTrace> traces = new HttpTraceEndpoint(repository).traces().getTraces();
		assertThat(traces).hasSize(1);
		HttpTrace trace = traces.get(0);
		assertThat(trace.getRequest().getMethod()).isEqualTo("GET");
	}

	private TraceableRequest createRequest(String method) {
		TraceableRequest request = mock(TraceableRequest.class);
		given(request.getMethod()).willReturn(method);
		return request;
	}

}
