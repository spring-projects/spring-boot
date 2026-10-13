/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.graphql.test.autoconfigure.tester;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.graphql.test.tester.HttpGraphQlTester;

/**
 * Annotation that can be applied to a test class to enable an {@link HttpGraphQlTester}.
 *
 * @author Brian Clozel
 * @since 4.0.0
 * @see HttpGraphQlTesterAutoConfiguration
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@AutoConfigureWebTestClient
@ImportAutoConfiguration
public @interface AutoConfigureHttpGraphQlTester {

}
