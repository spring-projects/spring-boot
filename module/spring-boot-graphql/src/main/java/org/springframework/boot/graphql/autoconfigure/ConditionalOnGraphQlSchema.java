/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.graphql.autoconfigure;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Conditional;

/**
 * {@link Conditional @Conditional} that only matches when a GraphQL schema is defined for
 * the application, through schema files or infrastructure beans.
 *
 * @author Brian Clozel
 * @since 4.0.0
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Conditional(DefaultGraphQlSchemaCondition.class)
public @interface ConditionalOnGraphQlSchema {

}
