/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.autoconfigure.web;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Conditional;

/**
 * {@link Conditional @Conditional} that checks whether the Spring resource handling chain
 * is enabled. Matches if {@link WebProperties.Resources.Chain#getEnabled()} is
 * {@code true} or if one of {@code "org.webjars:webjars-locator-core"},
 * {@code "org.webjars:webjars-locator-lite"} is on the classpath.
 * <p>
 * Note that support for {@code "org.webjars:webjars-locator-core"} is deprecated.
 *
 * @author Stephane Nicoll
 * @since 1.3.0
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Conditional(OnEnabledResourceChainCondition.class)
public @interface ConditionalOnEnabledResourceChain {

}
