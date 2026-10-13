/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.configurationsample.generic;

import java.time.Duration;

import org.springframework.boot.configurationsample.TestConfigurationProperties;

/**
 * Simple properties with resolved generic information.
 *
 * @author Stephane Nicoll
 */
@TestConfigurationProperties("generic")
public class SimpleGenericProperties extends AbstractIntermediateGenericProperties<Duration> {

}
