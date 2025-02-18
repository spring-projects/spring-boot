/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.actuate.metrics.data;

import java.lang.reflect.Method;

import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.MockClock;
import io.micrometer.core.instrument.simple.SimpleConfig;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.metrics.AutoTimer;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.support.RepositoryMethodInvocationListener.RepositoryMethodInvocation;
import org.springframework.data.repository.core.support.RepositoryMethodInvocationListener.RepositoryMethodInvocationResult;
import org.springframework.data.repository.core.support.RepositoryMethodInvocationListener.RepositoryMethodInvocationResult.State;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link MetricsRepositoryMethodInvocationListener}.
 *
 * @author Phillip Webb
 */
class MetricsRepositoryMethodInvocationListenerTests {

	private static final String REQUEST_METRICS_NAME = "repository.invocations";

	private SimpleMeterRegistry registry;

	private MetricsRepositoryMethodInvocationListener listener;

	@BeforeEach
	void setup() {
		MockClock clock = new MockClock();
		this.registry = new SimpleMeterRegistry(SimpleConfig.DEFAULT, clock);
		this.listener = new MetricsRepositoryMethodInvocationListener(() -> this.registry,
				new DefaultRepositoryTagsProvider(), REQUEST_METRICS_NAME, AutoTimer.ENABLED);
	}

	@Test
	void afterInvocationWhenNoTimerAnnotationsAndNoAutoTimerDoesNothing() {
		this.listener = new MetricsRepositoryMethodInvocationListener(() -> this.registry,
				new DefaultRepositoryTagsProvider(), REQUEST_METRICS_NAME, null);
		this.listener.afterInvocation(createInvocation(NoAnnotationsRepository.class));
		assertThat(this.registry.find(REQUEST_METRICS_NAME).timers()).isEmpty();
	}

	@Test
	void afterInvocationWhenTimedMethodRecordsMetrics() {
		this.listener.afterInvocation(createInvocation(TimedMethodRepository.class));
		assertMetricsContainsTag("state", "SUCCESS");
		assertMetricsContainsTag("tag1", "value1");
	}

	@Test
	void afterInvocationWhenTimedClassRecordsMetrics() {
		this.listener.afterInvocation(createInvocation(TimedClassRepository.class));
		assertMetricsContainsTag("state", "SUCCESS");
		assertMetricsContainsTag("taga", "valuea");
	}

	@Test
	void afterInvocationWhenAutoTimedRecordsMetrics() {
		this.listener.afterInvocation(createInvocation(NoAnnotationsRepository.class));
		assertMetricsContainsTag("state", "SUCCESS");
	}

	private void assertMetricsContainsTag(String tagKey, String tagValue) {
		assertThat(this.registry.get(REQUEST_METRICS_NAME).tag(tagKey, tagValue).timer().count()).isOne();
	}

	private RepositoryMethodInvocation createInvocation(Class<?> repositoryInterface) {
		Method method = ReflectionUtils.findMethod(repositoryInterface, "findById", long.class);
		RepositoryMethodInvocationResult result = mock(RepositoryMethodInvocationResult.class);
		given(result.getState()).willReturn(State.SUCCESS);
		return new RepositoryMethodInvocation(repositoryInterface, method, result, 0);
	}

	interface NoAnnotationsRepository extends Repository<Example, Long> {

		Example findById(long id);

	}

	interface TimedMethodRepository extends Repository<Example, Long> {

		@Timed(extraTags = { "tag1", "value1" })
		Example findById(long id);

	}

	@Timed(extraTags = { "taga", "valuea" })
	interface TimedClassRepository extends Repository<Example, Long> {

		Example findById(long id);

	}

	static class Example {

	}

}
