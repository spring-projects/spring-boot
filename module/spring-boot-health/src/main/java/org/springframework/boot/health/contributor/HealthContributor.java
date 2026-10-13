/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.health.contributor;

/**
 * Contributes health information, either directly ({@link HealthIndicator}) or via other
 * contributors ({@link CompositeHealthContributor}).
 *
 * @author Phillip Webb
 * @since 4.0.0
 * @see HealthIndicator
 * @see CompositeHealthContributor
 */
public sealed interface HealthContributor permits HealthIndicator, CompositeHealthContributor {

}
