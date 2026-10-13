/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.couchbase.autoconfigure.health;

import com.couchbase.client.java.Cluster;
import reactor.core.publisher.Flux;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.couchbase.autoconfigure.CouchbaseAutoConfiguration;
import org.springframework.boot.couchbase.health.CouchbaseReactiveHealthIndicator;
import org.springframework.boot.health.autoconfigure.contributor.CompositeReactiveHealthContributorConfiguration;
import org.springframework.boot.health.autoconfigure.contributor.ConditionalOnEnabledHealthIndicator;
import org.springframework.boot.health.contributor.ReactiveHealthContributor;
import org.springframework.context.annotation.Bean;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for
 * {@link CouchbaseReactiveHealthIndicator}.
 *
 * @author Mikalai Lushchytski
 * @author Stephane Nicoll
 * @since 4.0.0
 */
@AutoConfiguration(after = CouchbaseAutoConfiguration.class)
@ConditionalOnClass({ Cluster.class, Flux.class, CouchbaseReactiveHealthIndicator.class,
		ConditionalOnEnabledHealthIndicator.class })
@ConditionalOnBean(Cluster.class)
@ConditionalOnEnabledHealthIndicator("couchbase")
public final class CouchbaseReactiveHealthContributorAutoConfiguration
		extends CompositeReactiveHealthContributorConfiguration<CouchbaseReactiveHealthIndicator, Cluster> {

	CouchbaseReactiveHealthContributorAutoConfiguration() {
		super(CouchbaseReactiveHealthIndicator::new);
	}

	@Bean
	@ConditionalOnMissingBean(name = { "couchbaseHealthIndicator", "couchbaseHealthContributor" })
	ReactiveHealthContributor couchbaseHealthContributor(ConfigurableListableBeanFactory beanFactory) {
		return createContributor(beanFactory, Cluster.class);
	}

}
