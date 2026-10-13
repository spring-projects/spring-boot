/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jms.autoconfigure;

import java.util.List;

import jakarta.jms.ConnectionFactory;
import jakarta.jms.Message;
import org.jspecify.annotations.Nullable;

import org.springframework.aot.hint.ExecutableMode;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jms.autoconfigure.JmsAutoConfiguration.JmsRuntimeHints;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.jms.core.JmsTemplate;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Spring JMS.
 *
 * @author Greg Turnquist
 * @author Stephane Nicoll
 * @author Vedran Pavic
 * @since 4.0.0
 */
@AutoConfiguration
@ConditionalOnClass({ Message.class, JmsTemplate.class })
@ConditionalOnBean(ConnectionFactory.class)
@EnableConfigurationProperties(JmsProperties.class)
@Import({ JmsClientConfigurations.JmsTemplateConfiguration.class,
		JmsClientConfigurations.MessagingTemplateConfiguration.class,
		JmsClientConfigurations.JmsClientConfiguration.class, JmsAnnotationDrivenConfiguration.class })
@ImportRuntimeHints(JmsRuntimeHints.class)
public final class JmsAutoConfiguration {

	static class JmsRuntimeHints implements RuntimeHintsRegistrar {

		@Override
		public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
			hints.reflection()
				.registerType(TypeReference.of(AcknowledgeMode.class), (type) -> type.withMethod("of",
						List.of(TypeReference.of(String.class)), ExecutableMode.INVOKE));
		}

	}

}
