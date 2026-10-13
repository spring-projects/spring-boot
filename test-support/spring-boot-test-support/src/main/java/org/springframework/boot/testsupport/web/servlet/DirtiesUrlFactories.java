/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.testsupport.web.servlet;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Indicates that a test pollutes URL factories.
 *
 * @author Phillip Webb
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
@Documented
@ExtendWith(DirtiesUrlFactoriesExtension.class)
public @interface DirtiesUrlFactories {

}
