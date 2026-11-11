/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.cassandra.autoconfigure.health;

import com.datastax.oss.driver.api.core.CqlSession;
import reactor.core.publisher.Flux;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.cassandra.autoconfigure.CassandraAutoConfiguration;
import org.springframework.boot.cassandra.autoconfigure.health.CassandraHealthContributorConfigurations.CassandraReactiveDriverConfiguration;
import org.springframework.boot.cassandra.health.CassandraDriverReactiveHealthIndicator;
import org.springframework.boot.health.autoconfigure.contributor.ConditionalOnEnabledHealthIndicator;
import org.springframework.context.annotation.Import;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for
 * {@link CassandraDriverReactiveHealthIndicator}.
 *
 * @author Artsiom Yudovin
 * @author Stephane Nicoll
 * @since 4.0.0
 */
@AutoConfiguration(after = CassandraAutoConfiguration.class)
@ConditionalOnClass({ CqlSession.class, Flux.class, CassandraDriverReactiveHealthIndicator.class,
		ConditionalOnEnabledHealthIndicator.class })
@ConditionalOnEnabledHealthIndicator("cassandra")
@Import(CassandraReactiveDriverConfiguration.class)
public final class CassandraReactiveHealthContributorAutoConfiguration {

}
