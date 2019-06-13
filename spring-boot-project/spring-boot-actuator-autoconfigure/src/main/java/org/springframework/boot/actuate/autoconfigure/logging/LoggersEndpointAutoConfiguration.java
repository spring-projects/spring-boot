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

package org.springframework.boot.actuate.autoconfigure.logging;

import org.springframework.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnAvailableEndpoint;
import org.springframework.boot.actuate.logging.LoggersEndpoint;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionMessage;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for the {@link LoggersEndpoint}.
 *
 * @author Phillip Webb
 * @since 2.0.0
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnAvailableEndpoint(endpoint = LoggersEndpoint.class)
public class LoggersEndpointAutoConfiguration {

	@Bean
	@ConditionalOnBean(LoggingSystem.class)
	@Conditional(OnEnabledLoggingSystemCondition.class)
	@ConditionalOnMissingBean
	public LoggersEndpoint loggersEndpoint(LoggingSystem loggingSystem) {
		return new LoggersEndpoint(loggingSystem);
	}

	static class OnEnabledLoggingSystemCondition extends SpringBootCondition {

		@Override
		public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
			ConditionMessage.Builder message = ConditionMessage.forCondition("Logging System");
			String loggingSystem = System.getProperty(LoggingSystem.SYSTEM_PROPERTY);
			if (LoggingSystem.NONE.equals(loggingSystem)) {
				return ConditionOutcome.noMatch(
						message.because("system property " + LoggingSystem.SYSTEM_PROPERTY + " is set to none"));
			}
			return ConditionOutcome.match(message.because("enabled"));
		}

	}

}
