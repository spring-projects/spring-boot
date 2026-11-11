/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.graphql.test.autoconfigure;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.autoconfigure.ImportAutoConfiguration;

/**
 * {@link ImportAutoConfiguration Auto-configuration imports} for typical Spring GraphQL
 * tests. Most tests should consider using {@link GraphQlTest @GraphQlTest} rather than
 * using this annotation directly.
 *
 * @author Brian Clozel
 * @since 4.0.0
 * @see GraphQlTest
 * @see org.springframework.boot.http.codec.autoconfigure.CodecsAutoConfiguration
 * @see org.springframework.boot.validation.autoconfigure.ValidationAutoConfiguration
 * @see org.springframework.boot.graphql.autoconfigure.GraphQlAutoConfiguration
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@ImportAutoConfiguration
public @interface AutoConfigureGraphQl {

}
