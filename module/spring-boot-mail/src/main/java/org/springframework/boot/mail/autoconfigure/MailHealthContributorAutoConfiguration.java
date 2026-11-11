/*
 * Copyright 2012-present the original author or authors.
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
public final class MailHealthContributorAutoConfiguration
		extends CompositeHealthContributorConfiguration<MailHealthIndicator, JavaMailSenderImpl> {

	MailHealthContributorAutoConfiguration() {
		super(MailHealthIndicator::new);
	}

	@Bean
	@ConditionalOnMissingBean(name = { "mailHealthIndicator", "mailHealthContributor" })
	HealthContributor mailHealthContributor(ConfigurableListableBeanFactory beanFactory) {
		return createContributor(beanFactory, JavaMailSenderImpl.class);
	}

}
