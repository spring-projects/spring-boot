/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.neo4j.autoconfigure.health;

import org.neo4j.driver.Driver;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.health.autoconfigure.contributor.ConditionalOnEnabledHealthIndicator;
import org.springframework.boot.neo4j.autoconfigure.Neo4jAutoConfiguration;
import org.springframework.boot.neo4j.autoconfigure.health.Neo4jHealthContributorConfigurations.Neo4jConfiguration;
import org.springframework.boot.neo4j.autoconfigure.health.Neo4jHealthContributorConfigurations.Neo4jReactiveConfiguration;
import org.springframework.boot.neo4j.health.Neo4jHealthIndicator;
import org.springframework.boot.neo4j.health.Neo4jReactiveHealthIndicator;
import org.springframework.context.annotation.Import;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for
 * {@link Neo4jReactiveHealthIndicator} and {@link Neo4jHealthIndicator}.
 *
 * @author Eric Spiegelberg
 * @author Stephane Nicoll
 * @author Michael J. Simons
 * @since 4.0.0
 */
@AutoConfiguration(after = Neo4jAutoConfiguration.class)
@ConditionalOnClass({ Driver.class, ConditionalOnEnabledHealthIndicator.class })
@ConditionalOnBean(Driver.class)
@ConditionalOnEnabledHealthIndicator("neo4j")
@Import({ Neo4jReactiveConfiguration.class, Neo4jConfiguration.class })
public final class Neo4jHealthContributorAutoConfiguration {

}
