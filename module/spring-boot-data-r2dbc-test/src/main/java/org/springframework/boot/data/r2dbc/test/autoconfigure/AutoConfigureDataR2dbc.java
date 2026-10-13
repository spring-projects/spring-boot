/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.data.r2dbc.test.autoconfigure;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureDataSourceInitialization;

/**
 * {@link ImportAutoConfiguration Auto-configuration imports} for typical Data R2DBC
 * tests. Most tests should consider using {@link DataR2dbcTest @DataR2dbcTest} rather
 * than using this annotation directly.
 *
 * @author Mark Paluch
 * @see DataR2dbcTest
 * @since 4.0.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@AutoConfigureDataSourceInitialization
@ImportAutoConfiguration
public @interface AutoConfigureDataR2dbc {

}
