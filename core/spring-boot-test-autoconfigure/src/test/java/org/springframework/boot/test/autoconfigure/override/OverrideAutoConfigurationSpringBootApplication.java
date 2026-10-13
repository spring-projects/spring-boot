/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.test.autoconfigure.override;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.OverrideAutoConfiguration;

/**
 * Example {@link SpringBootApplication @SpringBootApplication} for use with
 * {@link OverrideAutoConfiguration @OverrideAutoConfiguration} tests.
 *
 * @author Andy Wilkinson
 */
@SpringBootConfiguration
@EnableAutoConfiguration
public class OverrideAutoConfigurationSpringBootApplication {

}
