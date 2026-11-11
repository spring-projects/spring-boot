/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.transaction.jta.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.transaction.autoconfigure.TransactionAutoConfiguration;
import org.springframework.boot.transaction.autoconfigure.TransactionManagerCustomizationAutoConfiguration;
import org.springframework.context.annotation.Import;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for JTA.
 *
 * @author Josh Long
 * @author Phillip Webb
 * @author Nishant Raut
 * @since 4.0.0
 */
@AutoConfiguration(
		before = { TransactionAutoConfiguration.class, TransactionManagerCustomizationAutoConfiguration.class })
@ConditionalOnClass(jakarta.transaction.Transaction.class)
@ConditionalOnBooleanProperty(name = "spring.jta.enabled", matchIfMissing = true)
@Import(JndiJtaConfiguration.class)
public final class JtaAutoConfiguration {

}
