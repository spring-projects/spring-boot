/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.metrics;

import io.micrometer.core.instrument.Meter.Type;
import org.junit.Test;

import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ServiceLevelAgreementBoundary}.
 *
 * @author Phillip Webb
 */
public class ServiceLevelAgreementBoundaryTests {

	@Test
	public void getValueForDistributionSummaryWhenFromLongShouldReturnLongValue() {
		ServiceLevelAgreementBoundary sla = ServiceLevelAgreementBoundary.valueOf(123L);
		assertThat(sla.getValue(Type.DISTRIBUTION_SUMMARY)).isEqualTo(123);
	}

	@Test
	public void getValueForDistributionSummaryWhenFromNumberStringShouldReturnLongValue() {
		ServiceLevelAgreementBoundary sla = ServiceLevelAgreementBoundary.valueOf("123");
		assertThat(sla.getValue(Type.DISTRIBUTION_SUMMARY)).isEqualTo(123);
	}

	@Test
	public void getValueForDistributionSummaryWhenFromDurationStringShouldReturnNull() {
		ServiceLevelAgreementBoundary sla = ServiceLevelAgreementBoundary.valueOf("123ms");
		assertThat(sla.getValue(Type.DISTRIBUTION_SUMMARY)).isNull();
	}

	@Test
	public void getValueForTimerWhenFromLongShouldReturnMsToNanosValue() {
		ServiceLevelAgreementBoundary sla = ServiceLevelAgreementBoundary.valueOf(123L);
		assertThat(sla.getValue(Type.TIMER)).isEqualTo(123000000);
	}

	@Test
	public void getValueForTimerWhenFromNumberStringShouldMsToNanosValue() {
		ServiceLevelAgreementBoundary sla = ServiceLevelAgreementBoundary.valueOf("123");
		assertThat(sla.getValue(Type.TIMER)).isEqualTo(123000000);
	}

	@Test
	public void getValueForTimerWhenFromDurationStringShouldReturnDurationNanos() {
		ServiceLevelAgreementBoundary sla = ServiceLevelAgreementBoundary.valueOf("123ms");
		assertThat(sla.getValue(Type.TIMER)).isEqualTo(123000000);
	}

	@Test
	public void getValueForOthersShouldReturnNull() {
		ServiceLevelAgreementBoundary sla = ServiceLevelAgreementBoundary.valueOf("123");
		assertThat(sla.getValue(Type.COUNTER)).isNull();
		assertThat(sla.getValue(Type.GAUGE)).isNull();
		assertThat(sla.getValue(Type.LONG_TASK_TIMER)).isNull();
		assertThat(sla.getValue(Type.OTHER)).isNull();
	}

	@Test
	public void valueOfShouldWorkInBinder() {
		MockEnvironment environment = new MockEnvironment();
		TestPropertyValues.of("duration=10ms", "long=20").applyTo(environment);
		assertThat(Binder.get(environment).bind("duration", Bindable.of(ServiceLevelAgreementBoundary.class)).get()
				.getValue(Type.TIMER)).isEqualTo(10000000);
		assertThat(Binder.get(environment).bind("long", Bindable.of(ServiceLevelAgreementBoundary.class)).get()
				.getValue(Type.TIMER)).isEqualTo(20000000);
	}

}
