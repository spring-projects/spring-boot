/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.testing.springbootapplications.jmx

import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.autoconfigure.jmx.JmxAutoConfiguration

@SpringBootConfiguration
@ImportAutoConfiguration(JmxAutoConfiguration::class)
class SampleApp

