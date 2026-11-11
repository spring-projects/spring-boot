/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.data.jdbc.test.autoconfigure;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureJdbc;

/**
 * {@link ImportAutoConfiguration Auto-configuration imports} for typical Data JDBC tests.
 * Most tests should consider using {@link DataJdbcTest @DataJdbcTest} rather than using
 * this annotation directly.
 *
 * @author Andy Wilkinson
 * @since 4.0.0
 * @see DataJdbcTest
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@AutoConfigureJdbc
@ImportAutoConfiguration
public @interface AutoConfigureDataJdbc {

}
