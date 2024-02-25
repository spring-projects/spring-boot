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

package org.springframework.boot.actuate.autoconfigure.logging;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnAvailableEndpoint;
import org.springframework.boot.actuate.logging.LoggersEndpoint;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionMessage;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.boot.logging.LoggerGroups;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for the {@link LoggersEndpoint}.
 *
 * @author Phillip Webb
 * @since 2.0.0
 */
@AutoConfiguration
@ConditionalOnAvailableEndpoint(endpoint = LoggersEndpoint.class)
public class LoggersEndpointAutoConfiguration {

	/**
     * Creates a LoggersEndpoint bean if a LoggingSystem bean is present and the OnEnabledLoggingSystemCondition is met.
     * If a LoggersEndpoint bean is already present, this method will not be called.
     * 
     * @param loggingSystem The LoggingSystem bean used by the LoggersEndpoint.
     * @param springBootLoggerGroups An ObjectProvider for the LoggerGroups bean used by the LoggersEndpoint.
     * @return The LoggersEndpoint bean.
     */
    @Bean
	@ConditionalOnBean(LoggingSystem.class)
	@Conditional(OnEnabledLoggingSystemCondition.class)
	@ConditionalOnMissingBean
	public LoggersEndpoint loggersEndpoint(LoggingSystem loggingSystem,
			ObjectProvider<LoggerGroups> springBootLoggerGroups) {
		return new LoggersEndpoint(loggingSystem, springBootLoggerGroups.getIfAvailable(LoggerGroups::new));
	}

	/**
     * OnEnabledLoggingSystemCondition class.
     */
    static class OnEnabledLoggingSystemCondition extends SpringBootCondition {

		/**
         * Determines the match outcome for the condition based on the logging system configuration.
         * 
         * @param context the condition context
         * @param metadata the annotated type metadata
         * @return the condition outcome indicating whether the logging system is enabled or not
         */
        @Override
		public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
			ConditionMessage.Builder message = ConditionMessage.forCondition("Logging System");
			String loggingSystem = System.getProperty(LoggingSystem.SYSTEM_PROPERTY);
			if (LoggingSystem.NONE.equals(loggingSystem)) {
				return ConditionOutcome
					.noMatch(message.because("system property " + LoggingSystem.SYSTEM_PROPERTY + " is set to none"));
			}
			return ConditionOutcome.match(message.because("enabled"));
		}

	}

}
