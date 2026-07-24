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

package org.springframework.boot.grpc.test.autoconfigure;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.ClassOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestClassOrder;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression-oriented tests for nested Spring Test contexts using
 * {@link AutoConfigureTestGrpcTransport}.
 * <p>
 * Models the multi-context scenario reported in spring-boot#50860: a top-level
 * test and a {@code @Nested} class with a different active profile must each
 * receive a distinct test-only in-process transport name instead of sharing a
 * JVM-wide static name.
 *
 * @author xing
 */
@ExtendWith(SpringExtension.class)
@AutoConfigureTestGrpcTransport
@TestClassOrder(ClassOrderer.OrderAnnotation.class)
@TestMethodOrder(OrderAnnotation.class)
class AutoConfigureTestGrpcTransportNestedTests {

	private static final AtomicReference<String> topLevelName = new AtomicReference<>();

	@Autowired
	private Environment environment;

	@Test
	@Order(1)
	void topLevelContextReceivesTestOnlyInProcessName() {
		String name = this.environment.getProperty(TestGrpcTransportContextCustomizer.INPROCESS_NAME_PROPERTY);
		assertThat(name).isNotBlank();
		topLevelName.set(Objects.requireNonNull(name));
		assertThat(this.environment.getProperty("spring.grpc.server.inprocess.name")).isNull();
	}

	@Nested
	@Order(2)
	@ActiveProfiles("oauth2-disabled")
	@TestMethodOrder(OrderAnnotation.class)
	class NestedProfileContext {

		@Autowired
		private Environment environment;

		@Test
		@Order(1)
		void nestedContextReceivesDistinctTestOnlyInProcessName() {
			String nestedName = this.environment
				.getProperty(TestGrpcTransportContextCustomizer.INPROCESS_NAME_PROPERTY);
			assertThat(nestedName).isNotBlank();
			assertThat(this.environment.getProperty("spring.grpc.server.inprocess.name")).isNull();
			assertThat(topLevelName.get()).as("top-level name should already have been captured").isNotBlank();
			assertThat(nestedName).isNotEqualTo(topLevelName.get());
		}

	}

}
