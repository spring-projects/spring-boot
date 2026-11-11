/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.autoconfigure.condition.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionEvaluationReport;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

/**
 * Uniquely named auto-configuration for {@link ConditionEvaluationReport} tests.
 *
 * @author Andy Wilkinson
 */
@AutoConfiguration
@ConditionalOnProperty("unique")
public final class UniqueShortNameAutoConfiguration {

}
