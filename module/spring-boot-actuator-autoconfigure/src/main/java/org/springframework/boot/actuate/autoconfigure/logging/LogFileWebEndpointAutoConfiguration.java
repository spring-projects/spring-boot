/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.autoconfigure.logging;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnAvailableEndpoint;
import org.springframework.boot.actuate.logging.LogFileWebEndpoint;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionMessage;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.logging.LogFile;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.StringUtils;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link LogFileWebEndpoint}.
 *
 * @author Andy Wilkinson
 * @author Christian Carriere-Tisseur
 * @since 2.0.0
 */
@AutoConfiguration
@ConditionalOnAvailableEndpoint(LogFileWebEndpoint.class)
@EnableConfigurationProperties(LogFileWebEndpointProperties.class)
public final class LogFileWebEndpointAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	@Conditional(LogFileCondition.class)
	LogFileWebEndpoint logFileWebEndpoint(ObjectProvider<LogFile> logFile, LogFileWebEndpointProperties properties) {
		return new LogFileWebEndpoint(logFile.getIfAvailable(), properties.getExternalFile());
	}

	private static final class LogFileCondition extends SpringBootCondition {

		@Override
		public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
			Environment environment = context.getEnvironment();
			String config = getLogFileConfig(environment, LogFile.FILE_NAME_PROPERTY);
			ConditionMessage.Builder message = ConditionMessage.forCondition("Log File");
			if (StringUtils.hasText(config)) {
				return ConditionOutcome.match(message.found(LogFile.FILE_NAME_PROPERTY).items(config));
			}
			config = getLogFileConfig(environment, LogFile.FILE_PATH_PROPERTY);
			if (StringUtils.hasText(config)) {
				return ConditionOutcome.match(message.found(LogFile.FILE_PATH_PROPERTY).items(config));
			}
			config = environment.getProperty("management.endpoint.logfile.external-file");
			if (StringUtils.hasText(config)) {
				return ConditionOutcome.match(message.found("management.endpoint.logfile.external-file").items(config));
			}
			return ConditionOutcome.noMatch(message.didNotFind("logging file").atAll());
		}

		private String getLogFileConfig(Environment environment, String configName) {
			return environment.resolvePlaceholders("${" + configName + ":}");
		}

	}

}
