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
import org.springframework.boot.resttestclient.TestRestTemplate;

/**
 * Annotation that can be applied to a test class to enable auto-configuration of a
 * {@link TestRestTemplate}.
 *
 * @author Andy Wilkinson
 * @since 4.0.0
 * @deprecated since 4.2.0 for removal in 4.4.0 in favor of
 * {@link AutoConfigureRestTestClient}
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@ImportAutoConfiguration
@Deprecated(since = "4.2.0", forRemoval = true)
public @interface AutoConfigureTestRestTemplate {

}
