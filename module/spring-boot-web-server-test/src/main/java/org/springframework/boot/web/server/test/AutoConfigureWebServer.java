/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.web.server.test;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJson;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * {@link ImportAutoConfiguration Auto-configuration imports} for starting and embedded
 * web server for tests. Commonly used with {@link SpringBootTest @SpringBootTest} when
 * {@code classes} are specified and the {@code webEnvironment} attribute is
 * {@code RANDOM_PORT} or {@code DEFINED_PORT}.
 *
 * @author Phillip Webb
 * @since 4.1.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@AutoConfigureJson
@ImportAutoConfiguration
public @interface AutoConfigureWebServer {

}
