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

package org.springframework.boot.actuate.endpoint;

import java.util.Collections;

import org.junit.Test;
import org.springframework.boot.actuate.endpoint.TraceEndpoint;
import org.springframework.boot.actuate.trace.InMemoryTraceRepository;
import org.springframework.boot.actuate.trace.Trace;
import org.springframework.boot.actuate.trace.TraceRepository;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Tests for {@link TraceEndpoint}.
 * 
 * @author Phillip Webb
 */
public class TraceEndpointTests extends AbstractEndpointTests<TraceEndpoint> {

	public TraceEndpointTests() {
		super(Config.class, TraceEndpoint.class, "/trace", true, "endpoints.trace");
	}

	@Test
	public void invoke() throws Exception {
		Trace trace = getEndpointBean().invoke().get(0);
		assertThat(trace.getInfo().get("a"), equalTo((Object) "b"));
	}

	@Configuration
	@EnableConfigurationProperties
	public static class Config {

		@Bean
		public TraceEndpoint endpoint() {
			TraceRepository repository = new InMemoryTraceRepository();
			repository.add(Collections.<String, Object> singletonMap("a", "b"));
			return new TraceEndpoint(repository);
		}
	}
}
