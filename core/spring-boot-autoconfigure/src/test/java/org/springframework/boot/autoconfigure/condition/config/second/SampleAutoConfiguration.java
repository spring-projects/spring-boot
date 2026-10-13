/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.autoconfigure.condition.config.second;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionEvaluationReport;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * Sample auto-configuration for {@link ConditionEvaluationReport} tests.
 *
 * @author Madhura Bhave
 */
@AutoConfiguration("autoConfigTwo")
@ConditionalOnProperty("sample.second")
public final class SampleAutoConfiguration {

	@Bean
	String two() {
		return "two";
	}

}
