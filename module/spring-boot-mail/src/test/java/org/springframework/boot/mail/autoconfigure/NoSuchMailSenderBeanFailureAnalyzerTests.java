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

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.core.env.Environment;
import org.springframework.mail.MailSender;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link NoSuchMailSenderBeanFailureAnalyzer}.
 */
class NoSuchMailSenderBeanFailureAnalyzerTests {

	@Test
	void analyzeWhenNotNoSuchBeanDefinitionExceptionShouldReturnNull() {
		assertThat(new NoSuchMailSenderBeanFailureAnalyzer(null).analyze(new Exception())).isNull();
	}

	@Test
	void analyzeWhenNoSuchBeanDefinitionExceptionForDifferentTypeShouldReturnNull() {
		assertThat(
				new NoSuchMailSenderBeanFailureAnalyzer(null).analyze(new NoSuchBeanDefinitionException(String.class)))
			.isNull();
	}

	@Test
	void analyzeWhenMailHostPropertyIsConfiguredShouldReturnNull() {
		Environment environment = new MockEnvironment().withProperty("spring.mail.host", "smtp.example.org");
		assertThat(new NoSuchMailSenderBeanFailureAnalyzer(environment)
			.analyze(new NoSuchBeanDefinitionException(MailSender.class))).isNull();
	}

	@Test
	void analyzeWhenMailJndiNamePropertyIsConfiguredShouldReturnNull() {
		Environment environment = new MockEnvironment().withProperty("spring.mail.jndi-name", "mail/Session");
		assertThat(new NoSuchMailSenderBeanFailureAnalyzer(environment)
			.analyze(new NoSuchBeanDefinitionException(MailSender.class))).isNull();
	}

	@Test
	void analyzeWhenMailSenderBeanIsMissingAndNoMailPropertiesAreConfiguredShouldProvideGuidance() {
		FailureAnalysis analysis = new NoSuchMailSenderBeanFailureAnalyzer(new MockEnvironment())
			.analyze(new NoSuchBeanDefinitionException(MailSender.class));
		assertThat(analysis).isNotNull();
		assertThat(analysis.getDescription())
			.contains("A MailSender bean could not be found")
			.contains("spring.mail.host")
			.contains("spring.mail.jndi-name");
		assertThat(analysis.getAction())
			.contains("spring.mail.host")
			.contains("spring.mail.jndi-name")
			.contains("MailSender bean");
	}

	@Test
	void analyzeWhenJavaMailSenderBeanIsMissingAndNoMailPropertiesAreConfiguredShouldProvideGuidance() {
		assertThat(new NoSuchMailSenderBeanFailureAnalyzer(new MockEnvironment())
			.analyze(new NoSuchBeanDefinitionException(JavaMailSender.class))).isNotNull();
	}

}
