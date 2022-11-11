/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.observation.web.servlet;

import java.util.Collections;
import java.util.List;

import io.micrometer.common.KeyValue;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.observation.Observation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.metrics.web.servlet.DefaultWebMvcTagsProvider;
import org.springframework.boot.actuate.metrics.web.servlet.WebMvcTagsContributor;
import org.springframework.http.server.observation.ServerRequestObservationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.HandlerMapping;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ServerRequestObservationConventionAdapter}
 *
 * @author Brian Clozel
 */
@SuppressWarnings("removal")
@Deprecated(since = "3.0.0", forRemoval = true)
class ServerRequestObservationConventionAdapterTests {

	private static final String TEST_METRIC_NAME = "test.metric.name";

	private ServerRequestObservationConventionAdapter convention = new ServerRequestObservationConventionAdapter(
			TEST_METRIC_NAME, new DefaultWebMvcTagsProvider(), Collections.emptyList());

	private MockHttpServletRequest request = new MockHttpServletRequest("GET", "/resource/test");

	private MockHttpServletResponse response = new MockHttpServletResponse();

	private ServerRequestObservationContext context = new ServerRequestObservationContext(this.request, this.response);

	@Test
	void customNameIsUsed() {
		assertThat(this.convention.getName()).isEqualTo(TEST_METRIC_NAME);
	}

	@Test
	void onlySupportServerRequestObservationContext() {
		assertThat(this.convention.supportsContext(this.context)).isTrue();
		assertThat(this.convention.supportsContext(new OtherContext())).isFalse();
	}

	@Test
	void pushTagsAsLowCardinalityKeyValues() {
		this.request.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "/resource/{name}");
		this.context.setPathPattern("/resource/{name}");
		assertThat(this.convention.getLowCardinalityKeyValues(this.context)).contains(KeyValue.of("status", "200"),
				KeyValue.of("outcome", "SUCCESS"), KeyValue.of("uri", "/resource/{name}"),
				KeyValue.of("method", "GET"));
	}

	@Test
	void doesNotPushAnyHighCardinalityKeyValue() {
		assertThat(this.convention.getHighCardinalityKeyValues(this.context)).isEmpty();
	}

	@Test
	void pushTagsFromContributors() {
		ServerRequestObservationConventionAdapter convention = new ServerRequestObservationConventionAdapter(
				TEST_METRIC_NAME, null, List.of(new CustomWebMvcContributor()));
		assertThat(convention.getLowCardinalityKeyValues(this.context)).contains(KeyValue.of("custom", "value"));
	}

	static class OtherContext extends Observation.Context {

	}

	static class CustomWebMvcContributor implements WebMvcTagsContributor {

		@Override
		public Iterable<Tag> getTags(HttpServletRequest request, HttpServletResponse response, Object handler,
				Throwable exception) {
			return Tags.of("custom", "value");
		}

		@Override
		public Iterable<Tag> getLongRequestTags(HttpServletRequest request, Object handler) {
			return Collections.emptyList();
		}

	}

}
