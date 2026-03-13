/*
 * Copyright 2012-present the original author or authors.
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
