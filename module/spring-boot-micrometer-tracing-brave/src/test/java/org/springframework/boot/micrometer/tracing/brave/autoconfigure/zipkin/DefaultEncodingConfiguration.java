/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.micrometer.tracing.brave.autoconfigure.zipkin;

import zipkin2.reporter.Encoding;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.zipkin.autoconfigure.ZipkinAutoConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * Configures the bean {@linkplain ZipkinAutoConfiguration} would from properties.
 */
@TestConfiguration(proxyBeanMethods = false)
class DefaultEncodingConfiguration {

	@Bean
	@ConditionalOnMissingBean
	Encoding zipkinReporterEncoding() {
		return Encoding.JSON;
	}

}
