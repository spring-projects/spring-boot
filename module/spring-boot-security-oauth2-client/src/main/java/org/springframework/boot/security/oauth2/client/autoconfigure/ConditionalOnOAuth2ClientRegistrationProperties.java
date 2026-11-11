/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.security.oauth2.client.autoconfigure;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Conditional;

/**
 * Condition that matches if any {@code spring.security.oauth2.client.registration}
 * properties are defined.
 *
 * @author Andy Wilkinson
 * @since 4.0.0
 */
@SuppressWarnings("removal")
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
@Documented
@Conditional(ClientsConfiguredCondition.class)
public @interface ConditionalOnOAuth2ClientRegistrationProperties {

}
