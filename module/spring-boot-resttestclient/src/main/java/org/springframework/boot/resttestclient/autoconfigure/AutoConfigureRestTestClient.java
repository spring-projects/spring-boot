/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.resttestclient.autoconfigure;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.test.web.servlet.client.RestTestClient;

/**
 * Annotation that can be applied to a test class to enable a {@link RestTestClient}.
 *
 * @author Stephane Nicoll
 * @since 4.0.0
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@ImportAutoConfiguration
public @interface AutoConfigureRestTestClient {

}
