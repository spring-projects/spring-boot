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

package org.springframework.boot.testcontainers.lifecycle;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.testcontainers.lifecycle.Startable;

import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link TestcontainersStartup}.
 *
 * @author Phillip Webb
 */
class TestcontainersStartupTests {

	private static final String PROPERTY = TestcontainersStartup.PROPERTY;

	private final AtomicInteger counter = new AtomicInteger();

	@Test
	void startWhenSquentialStartsSequentially() {
		List<TestStartable> startables = createTestStartables(100);
		TestcontainersStartup.SEQUENTIAL.start(startables);
		for (int i = 0; i < startables.size(); i++) {
			assertThat(startables.get(i).getIndex()).isEqualTo(i);
			assertThat(startables.get(i).getThreadName()).isEqualTo(Thread.currentThread().getName());
		}
	}

	@Test
	void startWhenParallelStartsInParallel() {
		List<TestStartable> startables = createTestStartables(100);
		TestcontainersStartup.PARALLEL.start(startables);
		assertThat(startables.stream().map(TestStartable::getThreadName)).hasSizeGreaterThan(1);
	}

	@Test
	void getWhenNoPropertyReturnsDefault() {
		MockEnvironment environment = new MockEnvironment();
		assertThat(TestcontainersStartup.get(environment)).isEqualTo(TestcontainersStartup.SEQUENTIAL);
	}

	@Test
	void getWhenPropertyReturnsBasedOnValue() {
		MockEnvironment environment = new MockEnvironment();
		assertThat(TestcontainersStartup.get(environment.withProperty(PROPERTY, "SEQUENTIAL")))
			.isEqualTo(TestcontainersStartup.SEQUENTIAL);
		assertThat(TestcontainersStartup.get(environment.withProperty(PROPERTY, "sequential")))
			.isEqualTo(TestcontainersStartup.SEQUENTIAL);
		assertThat(TestcontainersStartup.get(environment.withProperty(PROPERTY, "SEQuenTIaL")))
			.isEqualTo(TestcontainersStartup.SEQUENTIAL);
		assertThat(TestcontainersStartup.get(environment.withProperty(PROPERTY, "S-E-Q-U-E-N-T-I-A-L")))
			.isEqualTo(TestcontainersStartup.SEQUENTIAL);
		assertThat(TestcontainersStartup.get(environment.withProperty(PROPERTY, "parallel")))
			.isEqualTo(TestcontainersStartup.PARALLEL);
	}

	@Test
	void getWhenUnknownPropertyThrowsException() {
		MockEnvironment environment = new MockEnvironment();
		assertThatIllegalArgumentException()
			.isThrownBy(() -> TestcontainersStartup.get(environment.withProperty(PROPERTY, "bad")))
			.withMessage("Unknown 'spring.testcontainers.beans.startup' property value 'bad'");
	}

	private List<TestStartable> createTestStartables(int size) {
		List<TestStartable> testStartables = new ArrayList<>(size);
		for (int i = 0; i < size; i++) {
			testStartables.add(new TestStartable());
		}
		return testStartables;
	}

	private final class TestStartable implements Startable {

		private int index;

		private String threadName;

		@Override
		public void start() {
			this.index = TestcontainersStartupTests.this.counter.getAndIncrement();
			this.threadName = Thread.currentThread().getName();
		}

		@Override
		public void stop() {
		}

		int getIndex() {
			return this.index;
		}

		String getThreadName() {
			return this.threadName;
		}

	}

}
