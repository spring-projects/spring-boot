/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.testing.springbootapplications.additionalautoconfigurationandslicing.composedannotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.integration.autoconfigure.IntegrationAutoConfiguration;
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest;

/**
 * A reusable JDBC test slice with Spring Integration auto-configuration.
 *
 * @author Dricmoy Bhattacharjee
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@JdbcTest
@ImportAutoConfiguration(IntegrationAutoConfiguration.class)
public @interface MyJdbcTest {

}
