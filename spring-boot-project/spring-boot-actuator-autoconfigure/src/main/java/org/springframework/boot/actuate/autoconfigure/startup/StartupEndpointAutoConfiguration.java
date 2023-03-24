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

package org.springframework.boot.actuate.autoconfigure.startup;

import org.springframework.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnAvailableEndpoint;
import org.springframework.boot.actuate.startup.StartupEndpoint;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionMessage;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.boot.context.metrics.buffering.BufferingApplicationStartup;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.core.metrics.ApplicationStartup;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for the {@link StartupEndpoint}.
 *
 * @author Brian Clozel
 * @since 2.4.0
 */
@AutoConfiguration
@ConditionalOnAvailableEndpoint(endpoint = StartupEndpoint.class)
@Conditional(StartupEndpointAutoConfiguration.ApplicationStartupCondition.class)
public class StartupEndpointAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public StartupEndpoint startupEndpoint(BufferingApplicationStartup applicationStartup) {
		return new StartupEndpoint(applicationStartup);
	}

	/**
	 * {@link SpringBootCondition} checking the configured
	 * {@link org.springframework.core.metrics.ApplicationStartup}.
	 * <p>
	 * Endpoint is enabled only if the configured implementation is
	 * {@link BufferingApplicationStartup}.
	 */
	static class ApplicationStartupCondition extends SpringBootCondition {

		@Override
		public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
			ConditionMessage.Builder message = ConditionMessage.forCondition("ApplicationStartup");
			ApplicationStartup applicationStartup = context.getBeanFactory().getApplicationStartup();
			if (applicationStartup instanceof BufferingApplicationStartup) {
				return ConditionOutcome
					.match(message.because("configured applicationStartup is of type BufferingApplicationStartup."));
			}
			return ConditionOutcome.noMatch(message.because("configured applicationStartup is of type "
					+ applicationStartup.getClass() + ", expected BufferingApplicationStartup."));
		}

	}

}
