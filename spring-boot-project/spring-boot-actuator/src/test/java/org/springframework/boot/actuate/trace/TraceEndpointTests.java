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

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link TraceEndpoint}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
public class TraceEndpointTests {

	@Test
	public void trace() throws Exception {
		TraceRepository repository = new InMemoryTraceRepository();
		repository.add(Collections.<String, Object>singletonMap("a", "b"));
		Trace trace = new TraceEndpoint(repository).traces().getTraces().get(0);
		assertThat(trace.getInfo().get("a")).isEqualTo("b");
	}

}
