/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.micrometer.tracing.test.autoconfigure;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Example {@link SpringBootApplication @SpringBootApplication} for use with
 * {@link AutoConfigureTracing @AutoConfigureTracing} tests.
 *
 * @author Chris Bono
 * @author Moritz Halbritter
 */
@SpringBootConfiguration
@EnableAutoConfiguration
class AutoConfigureTracingSpringBootApplication {

}
