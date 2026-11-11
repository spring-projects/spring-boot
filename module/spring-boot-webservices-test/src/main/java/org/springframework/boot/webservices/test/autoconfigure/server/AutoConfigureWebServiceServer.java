/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.webservices.test.autoconfigure.server;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.autoconfigure.ImportAutoConfiguration;

/**
 * Annotation that can be applied to a test class to enable and configure
 * auto-configuration of web service servers endpoints.
 *
 * @author Daniil Razorenov
 * @since 4.0.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@ImportAutoConfiguration
public @interface AutoConfigureWebServiceServer {

}
