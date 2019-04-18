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

import java.util.List;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;

import org.springframework.boot.actuate.autoconfigure.metrics.CompositeMeterRegistryConfiguration.MultipleNonPrimaryMeterRegistriesCondition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.boot.autoconfigure.condition.NoneNestedConditions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Configuration for a {@link CompositeMeterRegistry}.
 *
 * @author Andy Wilkinson
 */
@Configuration(proxyBeanMethods = false)
@Conditional(MultipleNonPrimaryMeterRegistriesCondition.class)
class CompositeMeterRegistryConfiguration {

	@Bean
	@Primary
	public CompositeMeterRegistry compositeMeterRegistry(Clock clock,
			List<MeterRegistry> registries) {
		return new CompositeMeterRegistry(clock, registries);
	}

	static class MultipleNonPrimaryMeterRegistriesCondition extends NoneNestedConditions {

		MultipleNonPrimaryMeterRegistriesCondition() {
			super(ConfigurationPhase.REGISTER_BEAN);
		}

		@ConditionalOnMissingBean(MeterRegistry.class)
		static class NoMeterRegistryCondition {

		}

		@ConditionalOnSingleCandidate(MeterRegistry.class)
		static class SingleInjectableMeterRegistry {

		}

	}

}
