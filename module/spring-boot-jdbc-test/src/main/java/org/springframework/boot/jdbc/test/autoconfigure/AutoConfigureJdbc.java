/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jdbc.test.autoconfigure;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureDataSourceInitialization;

/**
 * {@link ImportAutoConfiguration Auto-configuration imports} for typical JDBC tests. Most
 * tests should consider using {@link JdbcTest @JdbcTest} rather than using this
 * annotation directly.
 *
 * @author Stephane Nicoll
 * @since 4.0.0
 * @see JdbcTest
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@ImportAutoConfiguration
@AutoConfigureDataSourceInitialization
public @interface AutoConfigureJdbc {

}
