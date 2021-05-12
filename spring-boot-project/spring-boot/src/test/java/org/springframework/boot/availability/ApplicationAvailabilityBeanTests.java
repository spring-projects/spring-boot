/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.availability;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ApplicationAvailabilityBean}
 *
 * @author Brian Clozel
 * @author Phillip Webb
 */
class ApplicationAvailabilityBeanTests {

	private AnnotationConfigApplicationContext context;

	private ApplicationAvailabilityBean availability;

	@BeforeEach
	void setup() {
		this.context = new AnnotationConfigApplicationContext(ApplicationAvailabilityBean.class);
		this.availability = this.context.getBean(ApplicationAvailabilityBean.class);
	}

	@Test
	void getLivenessStateWhenNoEventHasBeenPublishedReturnsDefaultState() {
		assertThat(this.availability.getLivenessState()).isEqualTo(LivenessState.BROKEN);
	}

	@Test
	void getLivenessStateWhenEventHasBeenPublishedReturnsPublishedState() {
		AvailabilityChangeEvent.publish(this.context, LivenessState.CORRECT);
		assertThat(this.availability.getLivenessState()).isEqualTo(LivenessState.CORRECT);
	}

	@Test
	void getReadinessStateWhenNoEventHasBeenPublishedReturnsDefaultState() {
		assertThat(this.availability.getReadinessState()).isEqualTo(ReadinessState.REFUSING_TRAFFIC);
	}

	@Test
	void getReadinessStateWhenEventHasBeenPublishedReturnsPublishedState() {
		AvailabilityChangeEvent.publish(this.context, ReadinessState.ACCEPTING_TRAFFIC);
		assertThat(this.availability.getReadinessState()).isEqualTo(ReadinessState.ACCEPTING_TRAFFIC);
	}

	@Test
	void getStateWhenNoEventHasBeenPublishedReturnsDefaultState() {
		assertThat(this.availability.getState(TestState.class)).isNull();
		assertThat(this.availability.getState(TestState.class, TestState.ONE)).isEqualTo(TestState.ONE);
	}

	@Test
	void getStateWhenEventHasBeenPublishedReturnsPublishedState() {
		AvailabilityChangeEvent.publish(this.context, TestState.TWO);
		assertThat(this.availability.getState(TestState.class)).isEqualTo(TestState.TWO);
		assertThat(this.availability.getState(TestState.class, TestState.ONE)).isEqualTo(TestState.TWO);
	}

	@Test
	void getLastChangeEventWhenNoEventHasBeenPublishedReturnsDefaultState() {
		assertThat(this.availability.getLastChangeEvent(TestState.class)).isNull();
	}

	@Test
	void getLastChangeEventWhenEventHasBeenPublishedReturnsPublishedState() {
		AvailabilityChangeEvent.publish(this.context, TestState.TWO);
		assertThat(this.availability.getLastChangeEvent(TestState.class)).isNotNull();
	}

	enum TestState implements AvailabilityState {

		ONE {
			@Override
			public String test() {
				return "spring";
			}
		},

		TWO {
			@Override
			public String test() {
				return "boot";
			}
		};

		abstract String test();

	}

}
