/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.grpc.server.autoconfigure;

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link OnEnabledGrpcServerCondition}.
 *
 * @author Chris Bono
 */
class OnEnabledGrpcServerConditionTests {

	@Test
	void shouldMatchIfNoPropertyIsSet() {
		OnEnabledGrpcServerCondition condition = new OnEnabledGrpcServerCondition();
		ConditionOutcome outcome = condition.getMatchOutcome(mockConditionContext(), mockMetadata(""));
		assertThat(outcome.isMatch()).isTrue();
		assertThat(outcome.getMessage())
			.isEqualTo("@ConditionalOnGrpcServerEnabled server and service are enabled by default");
	}

	@Test
	void shouldMatchIfOnlyGlobalPropertyIsSetAndIsTrue() {
		OnEnabledGrpcServerCondition condition = new OnEnabledGrpcServerCondition();
		ConditionOutcome outcome = condition
			.getMatchOutcome(mockConditionContext(Map.of("spring.grpc.server.enabled", "true")), mockMetadata(""));
		assertThat(outcome.isMatch()).isTrue();
		assertThat(outcome.getMessage())
			.isEqualTo("@ConditionalOnGrpcServerEnabled server and service are enabled by default");
	}

	@Test
	void shouldNotMatchIfOnlyGlobalPropertyIsSetAndIsFalse() {
		OnEnabledGrpcServerCondition condition = new OnEnabledGrpcServerCondition();
		ConditionOutcome outcome = condition
			.getMatchOutcome(mockConditionContext(Map.of("spring.grpc.server.enabled", "false")), mockMetadata(""));
		assertThat(outcome.isMatch()).isFalse();
		assertThat(outcome.getMessage())
			.isEqualTo("@ConditionalOnGrpcServerEnabled spring.grpc.server.enabled is false");
	}

	@Test
	void shouldMatchIfOnlyServicePropertyIsSetAndIsTrue() {
		OnEnabledGrpcServerCondition condition = new OnEnabledGrpcServerCondition();
		ConditionOutcome outcome = condition.getMatchOutcome(
				mockConditionContext(Map.of("spring.grpc.server.myservice.enabled", "true")),
				mockMetadata("myservice"));
		assertThat(outcome.isMatch()).isTrue();
		assertThat(outcome.getMessage())
			.isEqualTo("@ConditionalOnGrpcServerEnabled spring.grpc.server.myservice.enabled is true");
	}

	@Test
	void shouldNotMatchIfOnlyServicePropertyIsSetAndIsFalse() {
		OnEnabledGrpcServerCondition condition = new OnEnabledGrpcServerCondition();
		ConditionOutcome outcome = condition.getMatchOutcome(
				mockConditionContext(Map.of("spring.grpc.server.myservice.enabled", "false")),
				mockMetadata("myservice"));
		assertThat(outcome.isMatch()).isFalse();
		assertThat(outcome.getMessage())
			.isEqualTo("@ConditionalOnGrpcServerEnabled spring.grpc.server.myservice.enabled is false");
	}

	@Test
	void shouldMatchIfGlobalPropertyIsTrueAndServicePropertyIsTrue() {
		OnEnabledGrpcServerCondition condition = new OnEnabledGrpcServerCondition();
		ConditionOutcome outcome = condition.getMatchOutcome(
				mockConditionContext(
						Map.of("spring.grpc.server.enabled", "true", "spring.grpc.server.myservice.enabled", "true")),
				mockMetadata("myservice"));
		assertThat(outcome.isMatch()).isTrue();
		assertThat(outcome.getMessage())
			.isEqualTo("@ConditionalOnGrpcServerEnabled spring.grpc.server.myservice.enabled is true");
	}

	@Test
	void shouldNotMatchIfGlobalPropertyIsTrueAndServicePropertyIsFalse() {
		OnEnabledGrpcServerCondition condition = new OnEnabledGrpcServerCondition();
		ConditionOutcome outcome = condition.getMatchOutcome(
				mockConditionContext(
						Map.of("spring.grpc.server.enabled", "true", "spring.grpc.server.myservice.enabled", "false")),
				mockMetadata("myservice"));
		assertThat(outcome.isMatch()).isFalse();
		assertThat(outcome.getMessage())
			.isEqualTo("@ConditionalOnGrpcServerEnabled spring.grpc.server.myservice.enabled is false");
	}

	@Test
	void shouldNotMatchIfGlobalPropertyIsFalseAndServicePropertyIsTrue() {
		OnEnabledGrpcServerCondition condition = new OnEnabledGrpcServerCondition();
		ConditionOutcome outcome = condition.getMatchOutcome(
				mockConditionContext(
						Map.of("spring.grpc.server.enabled", "false", "spring.grpc.server.myservice.enabled", "true")),
				mockMetadata("myservice"));
		assertThat(outcome.isMatch()).isFalse();
		assertThat(outcome.getMessage())
			.isEqualTo("@ConditionalOnGrpcServerEnabled spring.grpc.server.enabled is false");
	}

	private ConditionContext mockConditionContext() {
		return mockConditionContext(Collections.emptyMap());
	}

	private ConditionContext mockConditionContext(Map<String, String> properties) {
		ConditionContext context = mock(ConditionContext.class);
		MockEnvironment environment = new MockEnvironment();
		properties.forEach(environment::setProperty);
		given(context.getEnvironment()).willReturn(environment);
		return context;
	}

	private AnnotatedTypeMetadata mockMetadata(String serviceName) {
		AnnotatedTypeMetadata metadata = mock(AnnotatedTypeMetadata.class);
		given(metadata.getAnnotationAttributes(ConditionalOnGrpcServerEnabled.class.getName()))
			.willReturn(Map.of("value", serviceName));
		return metadata;
	}

}
