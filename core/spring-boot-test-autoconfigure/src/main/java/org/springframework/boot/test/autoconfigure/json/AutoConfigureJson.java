/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.test.autoconfigure.json;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.autoconfigure.ImportAutoConfiguration;

/**
 * {@link ImportAutoConfiguration Auto-configuration imports} for typical JSON tests. Most
 * tests should consider using {@link JsonTest @JsonTest} rather than using this
 * annotation directly.
 *
 * @author Phillip Webb
 * @since 1.4.0
 * @see JsonTest
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@ImportAutoConfiguration
public @interface AutoConfigureJson {

}
