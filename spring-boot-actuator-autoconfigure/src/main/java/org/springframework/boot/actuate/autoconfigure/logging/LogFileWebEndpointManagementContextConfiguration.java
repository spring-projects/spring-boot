/*
 * Copyright 2012-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.actuate.autoconfigure.logging;

import org.springframework.boot.actuate.autoconfigure.web.ManagementContextConfiguration;
import org.springframework.boot.actuate.logger.LogFileWebEndpoint;
import org.springframework.boot.autoconfigure.condition.ConditionMessage;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.StringUtils;

/**
 * {@link ManagementContextConfiguration} for {@link LogFileWebEndpoint}.
 *
 * @author Andy Wilkinson
 * @since 2.0.0
 */
@ManagementContextConfiguration
@EnableConfigurationProperties(LogFileWebEndpointProperties.class)
public class LogFileWebEndpointManagementContextConfiguration {

	private final LogFileWebEndpointProperties properties;

	public LogFileWebEndpointManagementContextConfiguration(
			LogFileWebEndpointProperties properties) {
		this.properties = properties;
	}

	@Bean
	@ConditionalOnMissingBean
	@Conditional(LogFileCondition.class)
	public LogFileWebEndpoint logFileWebEndpoint(Environment environment) {
		return new LogFileWebEndpoint(environment, this.properties.getExternalFile());
	}

	private static class LogFileCondition extends SpringBootCondition {

		@Override
		public ConditionOutcome getMatchOutcome(ConditionContext context,
				AnnotatedTypeMetadata metadata) {
			Environment environment = context.getEnvironment();
			String config = environment.resolvePlaceholders("${logging.file:}");
			ConditionMessage.Builder message = ConditionMessage.forCondition("Log File");
			if (StringUtils.hasText(config)) {
				return ConditionOutcome
						.match(message.found("logging.file").items(config));
			}
			config = environment.resolvePlaceholders("${logging.path:}");
			if (StringUtils.hasText(config)) {
				return ConditionOutcome
						.match(message.found("logging.path").items(config));
			}
			config = environment.getProperty("endpoints.logfile.external-file");
			if (StringUtils.hasText(config)) {
				return ConditionOutcome.match(
						message.found("endpoints.logfile.external-file").items(config));
			}
			return ConditionOutcome.noMatch(message.didNotFind("logging file").atAll());
		}

	}

}
