/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.liquibase.autoconfigure;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.beans.factory.annotation.Qualifier;

/**
 * Qualifier annotation for a DataSource to be injected into Liquibase. If used for a
 * second data source, the other (main) one would normally be marked as {@code @Primary}.
 *
 * @author Eddú Meléndez
 * @since 4.0.0
 */
@Target({ ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.TYPE, ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Qualifier
public @interface LiquibaseDataSource {

}
