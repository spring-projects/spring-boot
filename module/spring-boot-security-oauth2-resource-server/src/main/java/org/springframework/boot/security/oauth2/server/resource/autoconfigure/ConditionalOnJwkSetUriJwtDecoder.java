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
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;

/**
 * Condition that matches when a JWK Set URI based {@link NimbusJwtDecoder} or
 * {@link NimbusReactiveJwtDecoder} should be used.
 *
 * @author Andy Wilkinson
 * @since 4.0.8
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
@Documented
@Conditional(JwkSetUriCondition.class)
public @interface ConditionalOnJwkSetUriJwtDecoder {

}
