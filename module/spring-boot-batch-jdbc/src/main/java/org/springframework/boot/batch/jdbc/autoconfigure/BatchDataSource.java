/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.batch.jdbc.autoconfigure;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;

/**
 * Qualifier annotation for a DataSource to be injected into Batch auto-configuration. Can
 * be used on a secondary data source, if there is another one marked as
 * {@link Primary @Primary}.
 *
 * @author Dmytro Nosan
 * @since 4.0.0
 */
@Target({ ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.TYPE, ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Qualifier
public @interface BatchDataSource {

}
