/*
 * Copyright 2012-2024 the original author or authors.
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

import io.micrometer.core.aop.CountedAspect;
import io.micrometer.core.aop.MeterTagAnnotationHandler;
import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;
import org.aspectj.weaver.Advice;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAspectsAutoConfiguration.ObservationAnnotationsEnabledCondition;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Micrometer-based metrics
 * aspects.
 *
 * @author Jonatan Ivanov
 * @since 3.2.0
 */
@AutoConfiguration(after = { MetricsAutoConfiguration.class, CompositeMeterRegistryAutoConfiguration.class })
@ConditionalOnClass({ MeterRegistry.class, Advice.class })
@Conditional(ObservationAnnotationsEnabledCondition.class)
@ConditionalOnBean(MeterRegistry.class)
public class MetricsAspectsAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	CountedAspect countedAspect(MeterRegistry registry) {
		return new CountedAspect(registry);
	}

	@Bean
	@ConditionalOnMissingBean
	TimedAspect timedAspect(MeterRegistry registry,
			ObjectProvider<MeterTagAnnotationHandler> meterTagAnnotationHandler) {
		TimedAspect timedAspect = new TimedAspect(registry);
		meterTagAnnotationHandler.ifAvailable(timedAspect::setMeterTagAnnotationHandler);
		return timedAspect;
	}

	static final class ObservationAnnotationsEnabledCondition extends AnyNestedCondition {

		ObservationAnnotationsEnabledCondition() {
			super(ConfigurationPhase.PARSE_CONFIGURATION);
		}

		@ConditionalOnProperty(prefix = "micrometer.observations.annotations", name = "enabled", havingValue = "true")
		static class MicrometerObservationsEnabledCondition {

		}

		@ConditionalOnProperty(prefix = "management.observations.annotations", name = "enabled", havingValue = "true")
		static class ManagementObservationsEnabledCondition {

		}

	}

}
