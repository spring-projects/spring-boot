/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.env;

import java.util.Collections;
import java.util.Random;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.spy;

/**
 * Tests for {@link RandomValuePropertySource}.
 *
 * @author Dave Syer
 * @author Matt Benson
 */
class RandomValuePropertySourceTests {

	private RandomValuePropertySource source = new RandomValuePropertySource();

	@Test
	void getPropertyWhenNotRandomReturnsNull() {
		assertThat(this.source.getProperty("foo")).isNull();
	}

	@Test
	void getPropertyWhenStringReturnsValue() {
		assertThat(this.source.getProperty("random.string")).isNotNull();
	}

	@Test
	void getPropertyWhenIntReturnsValue() {
		Integer value = (Integer) this.source.getProperty("random.int");
		assertThat(value).isNotNull();
	}

	@Test
	void getPropertyWhenUuidReturnsValue() {
		String value = (String) this.source.getProperty("random.uuid");
		assertThat(value).isNotNull();
		assertThat(UUID.fromString(value)).isNotNull();
	}

	@Test
	void getPropertyWhenIntRangeReturnsValue() {
		Integer value = (Integer) this.source.getProperty("random.int[4,10]");
		assertThat(value).isNotNull();
		assertThat(value >= 4).isTrue();
		assertThat(value < 10).isTrue();
	}

	@Test
	void intRangeWhenLowerBoundEqualsUpperBoundShouldFailWithIllegalArgumentException() {
		assertThatIllegalArgumentException().isThrownBy(() -> this.source.getProperty("random.int[4,4]"))
				.withMessage("Lower bound must be less than upper bound.");
	}

	@Test
	void intRangeWhenLowerBoundNegative() {
		Integer value = (Integer) this.source.getProperty("random.int[-4,4]");
		assertThat(value >= -4).isTrue();
		assertThat(value < 4).isTrue();
	}

	@Test
	void getPropertyWhenIntMaxReturnsValue() {
		Integer value = (Integer) this.source.getProperty("random.int(10)");
		assertThat(value).isNotNull().isLessThan(10);
	}

	@Test
	void intMaxZero() {
		assertThatIllegalArgumentException().isThrownBy(() -> this.source.getProperty("random.int(0)"))
				.withMessage("Bound must be positive.");
	}

	@Test
	void intNegativeBound() {
		assertThatIllegalArgumentException().isThrownBy(() -> this.source.getProperty("random.int(-5)"))
				.withMessage("Bound must be positive.");
	}

	@Test
	void getPropertyWhenLongReturnsValue() {
		Long value = (Long) this.source.getProperty("random.long");
		assertThat(value).isNotNull();
	}

	@Test
	void getPropertyWhenLongRangeReturnsValue() {
		Long value = (Long) this.source.getProperty("random.long[4,10]");
		assertThat(value).isNotNull().isBetween(4L, 10L);
	}

	@Test
	void longRangeWhenLowerBoundEqualsUpperBoundShouldFailWithIllegalArgumentException() {
		assertThatIllegalArgumentException().isThrownBy(() -> this.source.getProperty("random.long[4,4]"))
				.withMessage("Lower bound must be less than upper bound.");
	}

	@Test
	void longRangeWhenLowerBoundNegativeShouldFailWithIllegalArgumentException() {
		Long value = (Long) this.source.getProperty("random.long[-4,4]");
		assertThat(value >= -4).isTrue();
		assertThat(value < 4).isTrue();
	}

	@Test
	void getPropertyWhenLongMaxReturnsValue() {
		Long value = (Long) this.source.getProperty("random.long(10)");
		assertThat(value).isNotNull().isLessThan(10L);
	}

	@Test
	void longMaxZero() {
		assertThatIllegalArgumentException().isThrownBy(() -> this.source.getProperty("random.long(0)"))
				.withMessage("Bound must be positive.");
	}

	@Test
	void longNegativeBound() {
		assertThatIllegalArgumentException().isThrownBy(() -> this.source.getProperty("random.long(-5)"))
				.withMessage("Bound must be positive.");
	}

	@Test
	void getPropertyWhenLongOverflowReturnsValue() {
		RandomValuePropertySource source = spy(this.source);
		given(source.getSource()).willReturn(new Random() {

			@Override
			public long nextLong() {
				// constant that used to become -8, now becomes 8
				return Long.MIN_VALUE;
			}

		});
		Long value = (Long) source.getProperty("random.long(10)");
		assertThat(value).isNotNull().isGreaterThanOrEqualTo(0L).isLessThan(10L);
		value = (Long) source.getProperty("random.long[4,10]");
		assertThat(value).isNotNull().isGreaterThanOrEqualTo(4L).isLessThan(10L);
	}

	@Test
	void addToEnvironmentAddsSource() {
		MockEnvironment environment = new MockEnvironment();
		RandomValuePropertySource.addToEnvironment(environment);
		assertThat(environment.getProperty("random.string")).isNotNull();
	}

	@Test
	void addToEnvironmentWhenAlreadyAddedAddsSource() {
		MockEnvironment environment = new MockEnvironment();
		RandomValuePropertySource.addToEnvironment(environment);
		RandomValuePropertySource.addToEnvironment(environment);
		assertThat(environment.getProperty("random.string")).isNotNull();
	}

	@Test
	void addToEnvironmentAddsAfterSystemEnvironment() {
		MockEnvironment environment = new MockEnvironment();
		environment.getPropertySources().addFirst(new SystemEnvironmentPropertySource(
				StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME, Collections.emptyMap()));
		RandomValuePropertySource.addToEnvironment(environment);
		assertThat(environment.getPropertySources().stream().map(PropertySource::getName)).containsExactly(
				StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
				RandomValuePropertySource.RANDOM_PROPERTY_SOURCE_NAME, "mockProperties");
	}

}
