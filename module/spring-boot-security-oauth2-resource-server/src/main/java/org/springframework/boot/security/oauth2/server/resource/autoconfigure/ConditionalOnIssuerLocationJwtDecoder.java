/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.security.oauth2.server.resource.autoconfigure;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Conditional;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/**
 * Condition that matches when an {@link NimbusJwtDecoder#withIssuerLocation
 * issuer-location-based JWT decoder} should be used.
 *
 * @author Andy Wilkinson
 * @since 4.0.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
@Documented
@Conditional(IssuerUriCondition.class)
public @interface ConditionalOnIssuerLocationJwtDecoder {

}
