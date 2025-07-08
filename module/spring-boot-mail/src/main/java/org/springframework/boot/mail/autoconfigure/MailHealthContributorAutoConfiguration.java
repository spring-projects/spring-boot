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

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.health.autoconfigure.contributor.CompositeHealthContributorConfiguration;
import org.springframework.boot.health.autoconfigure.contributor.ConditionalOnEnabledHealthIndicator;
import org.springframework.boot.health.contributor.HealthContributor;
import org.springframework.boot.mail.health.MailHealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.mail.javamail.JavaMailSenderImpl;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link MailHealthIndicator}.
 *
 * @author Johannes Edmeier
 * @since 4.0.0
 */
@AutoConfiguration(after = MailSenderAutoConfiguration.class)
@ConditionalOnClass({ JavaMailSenderImpl.class, MailHealthIndicator.class, ConditionalOnEnabledHealthIndicator.class })
@ConditionalOnBean(JavaMailSenderImpl.class)
@ConditionalOnEnabledHealthIndicator("mail")
public class MailHealthContributorAutoConfiguration
		extends CompositeHealthContributorConfiguration<MailHealthIndicator, JavaMailSenderImpl> {

	public MailHealthContributorAutoConfiguration() {
		super(MailHealthIndicator::new);
	}

	@Bean
	@ConditionalOnMissingBean(name = { "mailHealthIndicator", "mailHealthContributor" })
	public HealthContributor mailHealthContributor(ConfigurableListableBeanFactory beanFactory) {
		return createContributor(beanFactory, JavaMailSenderImpl.class);
	}

}
