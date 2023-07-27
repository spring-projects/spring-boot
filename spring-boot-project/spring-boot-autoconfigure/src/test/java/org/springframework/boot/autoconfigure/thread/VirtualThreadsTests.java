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

package org.springframework.boot.autoconfigure.thread;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link VirtualThreads}.
 *
 * @author Moritz Halbritter
 */
class VirtualThreadsTests {

	@Test
	@EnabledForJreRange(max = JRE.JAVA_20)
	void shouldThrowExceptionBelowJava21() {
		assertThatThrownBy(VirtualThreads::new).isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Executors.newVirtualThreadPerTaskExecutor() method is missing");
	}

	@Test
	@EnabledForJreRange(min = JRE.JAVA_21)
	void shouldReturnExecutorOnJava21AndUp() {
		VirtualThreads virtualThreads = new VirtualThreads();
		assertThat(virtualThreads.getExecutor()).isNotNull();
	}

}
