/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.ldap.autoconfigure.health;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.health.autoconfigure.contributor.CompositeHealthContributorConfiguration;
import org.springframework.boot.health.autoconfigure.contributor.ConditionalOnEnabledHealthIndicator;
import org.springframework.boot.health.contributor.HealthContributor;
import org.springframework.boot.ldap.autoconfigure.LdapAutoConfiguration;
import org.springframework.boot.ldap.health.LdapHealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.ldap.core.LdapOperations;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link LdapHealthIndicator}.
 *
 * @author Eddú Meléndez
 * @author Stephane Nicoll
 * @since 4.0.0
 */
@AutoConfiguration(after = LdapAutoConfiguration.class)
@ConditionalOnClass({ LdapOperations.class, LdapHealthIndicator.class, ConditionalOnEnabledHealthIndicator.class })
@ConditionalOnBean(LdapOperations.class)
@ConditionalOnEnabledHealthIndicator("ldap")
public final class LdapHealthContributorAutoConfiguration
		extends CompositeHealthContributorConfiguration<LdapHealthIndicator, LdapOperations> {

	LdapHealthContributorAutoConfiguration() {
		super(LdapHealthIndicator::new);
	}

	@Bean
	@ConditionalOnMissingBean(name = { "ldapHealthIndicator", "ldapHealthContributor" })
	HealthContributor ldapHealthContributor(ConfigurableListableBeanFactory beanFactory) {
		return createContributor(beanFactory, LdapOperations.class);
	}

}
