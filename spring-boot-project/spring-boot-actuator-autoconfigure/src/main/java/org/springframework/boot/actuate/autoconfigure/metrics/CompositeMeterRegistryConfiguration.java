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

	/**
     * Creates a composite meter registry with the given clock and list of meter registries.
     * The composite meter registry combines the functionality of multiple meter registries into a single registry.
     * The primary meter registry is set as the default registry for the application.
     *
     * @param clock the clock used for measuring time
     * @param registries the list of meter registries to be combined into the composite registry
     * @return the composite meter registry
     */
    @Bean
	@Primary
	AutoConfiguredCompositeMeterRegistry compositeMeterRegistry(Clock clock, List<MeterRegistry> registries) {
		return new AutoConfiguredCompositeMeterRegistry(clock, registries);
	}

	/**
     * MultipleNonPrimaryMeterRegistriesCondition class.
     */
    static class MultipleNonPrimaryMeterRegistriesCondition extends NoneNestedConditions {

		/**
         * Constructor for the MultipleNonPrimaryMeterRegistriesCondition class.
         * 
         * Initializes the condition with the specified configuration phase.
         * 
         * @param configurationPhase The configuration phase for the condition.
         */
        MultipleNonPrimaryMeterRegistriesCondition() {
			super(ConfigurationPhase.REGISTER_BEAN);
		}

		/**
         * NoMeterRegistryCondition class.
         */
        @ConditionalOnMissingBean(MeterRegistry.class)
		static class NoMeterRegistryCondition {

		}

		/**
         * SingleInjectableMeterRegistry class.
         */
        @ConditionalOnSingleCandidate(MeterRegistry.class)
		static class SingleInjectableMeterRegistry {

		}

	}

}
