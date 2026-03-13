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

import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;
import org.springframework.mail.MailSender;

/**
 * An {@link AbstractFailureAnalyzer} that improves missing {@link MailSender} guidance
 * when mail auto-configuration is present but not activated.
 */
class NoSuchMailSenderBeanFailureAnalyzer extends AbstractFailureAnalyzer<NoSuchBeanDefinitionException>
		implements Ordered {

	private static final String MAIL_HOST_PROPERTY = "spring.mail.host";

	private static final String MAIL_JNDI_NAME_PROPERTY = "spring.mail.jndi-name";

	private final @Nullable Environment environment;

	NoSuchMailSenderBeanFailureAnalyzer(@Nullable Environment environment) {
		this.environment = environment;
	}

	@Override
	protected @Nullable FailureAnalysis analyze(Throwable rootFailure, NoSuchBeanDefinitionException cause) {
		if (!isMissingMailSenderBean(cause) || hasMailConfigurationProperty()) {
			return null;
		}
		String description = "A MailSender bean could not be found because Spring Boot mail auto-configuration "
				+ "did not match. Neither '" + MAIL_HOST_PROPERTY + "' nor '" + MAIL_JNDI_NAME_PROPERTY
				+ "' is configured.";
		String action = "Consider configuring '" + MAIL_HOST_PROPERTY + "' or '" + MAIL_JNDI_NAME_PROPERTY
				+ "' to enable auto-configuration. If you want to use a custom mail sender, define a MailSender "
				+ "bean in your configuration.";
		return new FailureAnalysis(description, action, cause);
	}

	private boolean isMissingMailSenderBean(NoSuchBeanDefinitionException cause) {
		Class<?> beanType = cause.getBeanType();
		if (beanType == null && cause.getResolvableType() != null) {
			beanType = cause.getResolvableType().resolve();
		}
		return (beanType != null) && MailSender.class.isAssignableFrom(beanType);
	}

	private boolean hasMailConfigurationProperty() {
		return this.environment != null && (this.environment.containsProperty(MAIL_HOST_PROPERTY)
				|| this.environment.containsProperty(MAIL_JNDI_NAME_PROPERTY));
	}

	@Override
	public int getOrder() {
		return 0;
	}

}
