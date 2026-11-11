/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.mail.autoconfigure;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.mail.MailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatException;

/**
 * Tests for {@link NoSuchMailSenderBeanFailureAnalyzer}.
 *
 * @author MJY (answndud)
 * @author Andy Wilkinson
 */
class NoSuchMailSenderBeanFailureAnalyzerTests {

	@Test
	void analyzeWhenNotNoSuchBeanDefinitionExceptionShouldReturnNull() {
		new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(MailSenderAutoConfiguration.class))
			.run((context) -> {
				ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
				FailureAnalysis analysis = new NoSuchMailSenderBeanFailureAnalyzer(beanFactory)
					.analyze(new Exception());
				assertThat(analysis).isNull();
			});
	}

	@Test
	void analyzeWhenNoSuchBeanDefinitionExceptionForDifferentTypeShouldReturnNull() {
		new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(MailSenderAutoConfiguration.class))
			.run((context) -> {
				ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
				assertThatException().isThrownBy(() -> context.getBean(String.class)).satisfies((ex) -> {
					FailureAnalysis analysis = new NoSuchMailSenderBeanFailureAnalyzer(beanFactory).analyze(ex);
					assertThat(analysis).isNull();
				});
			});
	}

	@Test
	void analyzeWithoutMailSenderAutoConfigurationShouldReturnNull() {
		new ApplicationContextRunner().run((context) -> {
			ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
			assertThatException().isThrownBy(() -> context.getBean(MailSender.class)).satisfies((ex) -> {
				FailureAnalysis analysis = new NoSuchMailSenderBeanFailureAnalyzer(beanFactory).analyze(ex);
				assertThat(analysis).isNull();
			});
		});
	}

	@Test
	void analyzeWhenMailSenderBeanIsMissingAndMailSenderConditionDidNotMatchShouldProvideGuidance() {
		new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(MailSenderAutoConfiguration.class))
			.run((context) -> {
				ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
				assertThatException().isThrownBy(() -> context.getBean(MailSender.class)).satisfies((ex) -> {
					FailureAnalysis analysis = new NoSuchMailSenderBeanFailureAnalyzer(beanFactory).analyze(ex);
					assertThat(analysis).isNotNull();
					assertThat(analysis.getDescription()).contains("A MailSender bean could not be found")
						.contains("spring.mail.host")
						.contains("spring.mail.jndi-name");
					assertThat(analysis.getAction()).contains("spring.mail.host")
						.contains("spring.mail.jndi-name")
						.contains("MailSender bean");
				});
			});
	}

}
