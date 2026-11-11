/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.webmvc.test.autoconfigure;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJson;

/**
 * {@link ImportAutoConfiguration Auto-configuration imports} for typical Spring MVC
 * tests. Most tests should consider using {@link WebMvcTest @WebMvcTest} rather than
 * using this annotation directly.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 4.0.0
 * @see WebMvcTest
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@AutoConfigureJson
@ImportAutoConfiguration
public @interface AutoConfigureWebMvc {

}
