/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.micrometer.metrics.test.autoconfigure;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Example {@link SpringBootApplication @SpringBootApplication} for use with
 * {@link AutoConfigureMetrics @AutoConfigureMetrics} tests.
 *
 * @author Chris Bono
 * @author Moritz Halbritter
 */
@SpringBootConfiguration
@EnableAutoConfiguration
class AutoConfigureMetricsSpringBootApplication {

}
