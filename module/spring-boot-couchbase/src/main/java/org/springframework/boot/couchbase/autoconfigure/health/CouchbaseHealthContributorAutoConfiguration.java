/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.couchbase.autoconfigure.health;

import com.couchbase.client.java.Cluster;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.couchbase.autoconfigure.CouchbaseAutoConfiguration;
import org.springframework.boot.couchbase.health.CouchbaseHealthIndicator;
import org.springframework.boot.health.autoconfigure.contributor.CompositeHealthContributorConfiguration;
import org.springframework.boot.health.autoconfigure.contributor.ConditionalOnEnabledHealthIndicator;
import org.springframework.boot.health.contributor.HealthContributor;
import org.springframework.context.annotation.Bean;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for
 * {@link CouchbaseHealthIndicator}.
 *
 * @author Eddú Meléndez
 * @author Stephane Nicoll
 * @author Andy Wilkinson Nicoll
 * @since 4.0.0
 */
@AutoConfiguration(
		after = { CouchbaseAutoConfiguration.class, CouchbaseReactiveHealthContributorAutoConfiguration.class })
@ConditionalOnClass({ Cluster.class, CouchbaseHealthIndicator.class, ConditionalOnEnabledHealthIndicator.class })
@ConditionalOnBean(Cluster.class)
@ConditionalOnEnabledHealthIndicator("couchbase")
public final class CouchbaseHealthContributorAutoConfiguration
		extends CompositeHealthContributorConfiguration<CouchbaseHealthIndicator, Cluster> {

	CouchbaseHealthContributorAutoConfiguration() {
		super(CouchbaseHealthIndicator::new);
	}

	@Bean
	@ConditionalOnMissingBean(name = { "couchbaseHealthIndicator", "couchbaseHealthContributor" })
	HealthContributor couchbaseHealthContributor(ConfigurableListableBeanFactory beanFactory) {
		return createContributor(beanFactory, Cluster.class);
	}

}
